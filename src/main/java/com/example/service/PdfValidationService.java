package com.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Serviço responsável por validar o PDF enviado e extrair informações básicas do aluno.
 * A validação inclui:
 * - Checagens estruturais (extensão, páginas, criptografia)
 * - Presença do link de autenticidade
 * - Download na API de autenticação da UFSC e comparação de hash entre a original e a cópia
 * - Extração de nome, matrícula e curso via expressões regulares
 */
@Service
public class PdfValidationService {

	// URL base para verificação de autenticidade (deve conter no PDF)
	private static final String UFSC_AUTHENTICATE_URL = "https://cagr.ufsc.br/autenticidade";

	// Regex do código de autenticação no formato 123456-12345678901234
	private static final Pattern CODE_PATTERN = Pattern.compile("([0-9]{6}-[0-9]{14})");

	/** Resultado da validação de PDF. 
	 * Nota: nome e curso são extraídos apenas para validar o PDF, mas não são armazenados.
	 */
	public record ValidationResult(boolean valid, String message, String matricula) {}

	/**
	 * Valida o arquivo PDF e tenta extrair os dados do aluno.
	 * @param file arquivo enviado pelo usuário
	 * @return {@link ValidationResult} com status, mensagem e dados extraídos
	 */
	public static ValidationResult validate(MultipartFile file) {
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
			return new ValidationResult(false, "O arquivo enviado não é um PDF.", null);
		}
		try (var is = file.getInputStream(); var doc = PDDocument.load(is)) {
			if (doc.getNumberOfPages() <= 0) {
				return new ValidationResult(false, "PDF inválido: nenhuma página encontrada.", null);
			}
			if (doc.isEncrypted()) {
				return new ValidationResult(false, "PDF está criptografado. Por favor, envie um PDF não criptografado.", null);
			}

			// Extrai o texto e valida a presença da URL de autenticidade + código
			String text = new PDFTextStripper().getText(doc);
			text = text.replaceAll("\\R+", " ").replaceAll("\\s+", " ");
			String code = getVerificationCode(text);
			if (code == null || !containsAuthenticateUrl(text)) {
				return new ValidationResult(false, "String de verificação não encontrada no PDF.", null);
			}

			// Download authenticity copy and compare
			byte[] originalBytes = file.getBytes();
			byte[] downloaded = downloadPdf(code);
			if (downloaded == null || downloaded.length == 0) {
				return new ValidationResult(false, "Não foi possível baixar o PDF de autenticidade para o código " + code + ".", null);
			}

			boolean equal = Arrays.equals(sha256(originalBytes), sha256(downloaded));
			if (!equal) {
				return new ValidationResult(false, "O conteúdo do PDF difere da cópia de autenticidade (código: " + code + ").", null);
			}
			// Extrai os detalhes do usuário do texto original do PDF para validação
			// Nota: nome e curso não são armazenados, apenas usados para validar o formato do PDF
			String nome = null, matricula = null, curso = null;
			Matcher nomeMatcherPTBR = Pattern.compile("arquivos, que\\s+([^,]+?)\\s*,").matcher(text);
			if (nomeMatcherPTBR.find()) {
				nome = nomeMatcherPTBR.group(1).trim();
			}
			final String INVALID_FILE = "Por favor, insira o arquivo PDF do atestado de matrícula em português";
			if(nome == null){
				return new ValidationResult(false, "Não foi possível extrair o nome do texto do PDF." + INVALID_FILE, null);
			}

			Matcher matriculaMatcherPTBR = Pattern
					.compile("sob o n[uú]mero\\s+([0-9\\s\\-]{8,})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
					.matcher(text);
			if (matriculaMatcherPTBR.find()) {
				matricula = matriculaMatcherPTBR.group(1).trim();
			} 
			if(matricula == null){
				return new ValidationResult(false, "Não foi possível extrair a matrícula do texto do PDF." + INVALID_FILE, null);
			}

			Matcher cursoMatcherPTBR = Pattern.compile("no curso de\\s+([^,]+?)\\s*,").matcher(text);
			if (cursoMatcherPTBR.find()) {
				curso = cursoMatcherPTBR.group(1).trim();
			}
			if(curso == null){
				return new ValidationResult(false, "Não foi possível extrair o curso do texto do PDF." + INVALID_FILE, null);
			}
			
			// Retorna apenas a matrícula - nome e curso foram validados mas não serão armazenados
			return new ValidationResult(true, "Arquivo PDF válido. URL: " + UFSC_AUTHENTICATE_URL + ", code: " + code, matricula);
		} catch (IOException e) {
			return new ValidationResult(false, "Falha ao ler o PDF: " + e.getMessage(), null);
		} catch (Exception e) {
			return new ValidationResult(false, "Falha ao baixar o PDF de autenticidade: " + e.getMessage(), null);
		}
	}

	/**
	 * Baixa o PDF de autenticidade a partir do código extraído. Faz uma requisição HTTP simulando o formulário web.
	 * @param code código de verificação extraído do PDF
	 * @return bytes do PDF baixado
	 */
	public static byte[] downloadPdf(String code) throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put("verificaAutForm", "verificaAutForm");
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://cagr.sistemas.ufsc.br/autenticidade/"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String cookie_to_post = getCookies(response);

        String response_body = response.body();
        String first  = "verificaAutForm:" + getCode(response_body, "<td class=\" col2\"><input type=\"text\" name=\"verificaAutForm:");
        params.put(first, code);
        
        String second  = "verificaAutForm:" + getCode(response_body, "<br /><input type=\"submit\" name=\"verificaAutForm:");
        params.put(second, "Verificar");

        String third  = getCode(response_body, "</div><input type=\"hidden\" name=\"javax.faces.ViewState\" id=\"javax.faces.ViewState\" value=\"");
        params.put("javax.faces.ViewState", third);
        

        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("https://cagr.sistemas.ufsc.br/autenticidade/"))
            .header("content-type", "application/x-www-form-urlencoded")
            .header("Cookie", cookie_to_post)
            .header("accept", "*/*")
            .header("accept-encoding", "gzip, deflate, br, zstd")
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(params)))
            .build();
        
        HttpResponse<byte[]> r2 = client.send(request2, BodyHandlers.ofByteArray());
 		  return r2.body();				
    }

    private static String getCookies(HttpResponse<String> response) {
        List<String> cookies = response.headers().allValues("set-cookie");
        String cookie_to_post = "";
        for(String cookie: cookies) {
            for(int i = 0; i < cookie.length(); i++) {
                cookie_to_post += cookie.charAt(i);
                if (cookie.charAt(i) == ';') {
                    break;
                }
            }
        }

        return cookie_to_post;
    }

	 
    private static String getCode(String body, String subString) {
        String ret = "";

        for (int i = body.indexOf(subString, 0) + subString.length(); i < body.length();i++) {
            if (body.charAt(i) == '"') {
                break;
            }
            ret+=body.charAt(i);
        }

        return ret;
    }

    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

	private static boolean containsAuthenticateUrl(String text) {
		return Pattern.compile("https?://(?:www\\.)?cagr\\.ufsc\\.br/autenticidade", Pattern.CASE_INSENSITIVE)
				.matcher(text)
				.find();
	}

	private static String getVerificationCode(String text) {
		Matcher matcher = CODE_PATTERN.matcher(text);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static byte[] sha256(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(data);
		} catch (Exception e) {
			return data; // fallback
		}
	}
}
