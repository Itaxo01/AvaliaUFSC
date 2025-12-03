package com.example.scrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.model.Disciplina;
import com.example.model.Professor;
import com.example.model.ScrapperStatus;
import com.example.service.ScrapperStatusService;
import com.example.service.DisciplinaService;
import com.example.service.ProfessorService;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Antigo scrapper de disciplinas do CAGR/UFSC.
 * A diferença é que esse utiliza login, o outro não precisa.
 * @deprecated 
 */
@Service
public class DisciplinaScrapperOld { 
    private static final Logger logger = LoggerFactory.getLogger(DisciplinaScrapperOld.class);
    
    private static final Object lock = new Object();
    
    // URLs e configurações
    private static final String BASE_URL = "https://cagr.sistemas.ufsc.br";
    private static final String LOGIN_URL = "https://sistemas.ufsc.br/login?service=https%3A%2F%2Fcagr.sistemas.ufsc.br%2Fj_spring_cas_security_check&userType=padrao&convertToUserType=alunoGraduacao&lockUserType=1";
    private static final String TURMAS_URL = BASE_URL + "/modules/aluno/cadastroTurmas/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    // Cliente HTTP configurado
    private final OkHttpClient httpClient;
    
    // Pattern para extrair ID Lattes
    
    @Autowired
    private DisciplinaService disciplinaService;
    
    @Autowired 
    private ProfessorService professorService;

    @Autowired
    private ScrapperStatusService scrapperStatusService;
    
    // Sets para evitar duplicatas
    
    public DisciplinaScrapperOld() {
        // Cliente HTTP com timeout e cookies
        java.net.CookieManager cookieManager = new java.net.CookieManager();
        cookieManager.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cookieJar(new okhttp3.JavaNetCookieJar(cookieManager))
                .build();
    }
    
    /**
     * Retorna o status atual do scrapper para interface administrativa
     */
    public ScrapperStatus getStatus() {
      return scrapperStatusService.getUltimoStatus();
    }
    
    /**
     * Executa o scraping com credenciais específicas e rastreamento do administrador
     */
    public ScrapingResult executarScraping(String user, String pass, String administrador) {
        synchronized (lock) {
            // Verifica se já está executando
            if (getStatus().isExecutando()) {
                throw new IllegalStateException("Scraping já está em execução desde " + getStatus().getUltimaExecucao());
            }
            
            logger.info("Iniciando scraping das disciplinas do CAGR/UFSC... Executado por: {}", administrador);
            scrapperStatusService.marcarInicioExecucao(administrador);
            // Atualiza status para executando
            
            ScrapingResult result = new ScrapingResult();
            try {
                // 1. Fazer login
                if (!fazerLogin(user, pass)) {
                    result.setErro("Falha no login");
                    scrapperStatusService.marcarFimExecucao(false, 0, 0, "Falha no login");
                    return result;
                }
                
                // 2. Selecionar os semestres e centros do formulário.
					 DadosIniciais dadosIniciais = obterDadosIniciais();
					 ArrayList<String> semestres = gerarSemestres(dadosIniciais.getSemestreAtual());
					 logger.info("Semestres a processar: {}", semestres);
					 Map<String, String> centros = dadosIniciais.getCentros();
					 logger.info("Centros encontrados: {}", centros.keySet());

					 // 3. Efetuar a busca para cada semestre e centro
					 for (String semestre : semestres) {
						logger.debug("Processando semestre: {}", semestre);
						for (Map.Entry<String, String> centro : centros.entrySet()) {
							String centroId = centro.getKey();
							String centroNome = centro.getValue();
							logger.debug("Processando centro: {} ({})", centroNome, centroId);
							
								try {
										Document resultDoc = efetuarBusca(semestre, centroId, result);
										if(resultDoc != null){
											// 4. Processar a tabela de dados
											logger.debug("Busca feita com sucesso, iniciando processamento das tabelas");
											processarPaginas(resultDoc, semestre, centroId, result);
										}

								} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
										logger.warn("Scraping interrompido");
								} catch (Exception e) {
										logger.warn("Erro ao processar semestre {} centro {}: {}", 
												semestre, centroNome, e.getMessage());
										continue; // Continua com o próximo curso
								}
							}
						}
						
						logger.info("Scraping concluído. Disciplinas: {}, Professores: {}", 
						result.getNumDisciplinasSalvas(), result.getNumProfessoresSalvos());
                
						// Atualiza status de sucesso
                scrapperStatusService.marcarFimExecucao(true, result.getNumDisciplinasSalvas(), result.getNumProfessoresSalvos(), null);

            } catch (Exception e) {
                logger.error("Erro durante o scraping", e);
                scrapperStatusService.marcarFimExecucao(false, 0, 0, "Erro durante o scraping: " + e.getMessage());
            } finally {
                // Sempre marca como não executando ao final
                scrapperStatusService.setExecucao(false);
            }
            return result;
        }
    }
    private boolean fazerLogin(String user, String pass) throws IOException {
        logger.info("Fazendo login no CAGR...");
        
        // Primeiro, obter a página de login para cookies e tokens
        Request getLoginPage = new Request.Builder()
                .url(LOGIN_URL)
                .header("User-Agent", USER_AGENT)
                .build();
                
        try (Response loginPageResponse = httpClient.newCall(getLoginPage).execute()) {
            if (!loginPageResponse.isSuccessful()) {
                logger.error("Erro ao carregar página de login: {}", loginPageResponse.code());
                return false;
            }
            
            // Parse da página para encontrar campos ocultos
            Document loginDoc = Jsoup.parse(loginPageResponse.body().string());
            
            // Construir formulário de login
            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("username", user)
                    .add("password", pass);
                    
            // Adicionar campos ocultos se existirem
            Elements hiddenInputs = loginDoc.select("input[type=hidden]");
            for (Element input : hiddenInputs) {
                String name = input.attr("name");
                String value = input.attr("value");
                if (!name.isEmpty()) {
                    formBuilder.add(name, value);
                }
            }
            
            // Enviar login
            Request loginRequest = new Request.Builder()
                    .url(LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", LOGIN_URL)
                    .post(formBuilder.build())
                    .build();
                    
            try (Response loginResponse = httpClient.newCall(loginRequest).execute()) {
                if (!loginResponse.isSuccessful()) {
                    logger.error("Erro na requisição de login: {}", loginResponse.code());
                    return false;
                }
                
                logger.info("Login enviado, verificando acesso...");
                
                // Verificar login tentando acessar a página de turmas
                return verificarAcessoAutenticado();
            }
        }
    }
    
    /**
     * Verifica se o login foi bem-sucedido tentando acessar a página de turmas
     */
    private boolean verificarAcessoAutenticado() throws IOException {
        Request testRequest = new Request.Builder()
                .url(TURMAS_URL)
                .header("User-Agent", USER_AGENT)
                .build();
        
        try (Response testResponse = httpClient.newCall(testRequest).execute()) {
            if (!testResponse.isSuccessful()) {
                logger.error("Erro ao testar acesso autenticado: {}", testResponse.code());
                return false;
            }
            
            String finalUrl = testResponse.request().url().toString();
            
				// Se conseguiu acessar a página de turmas, login foi bem-sucedido
				if (finalUrl.contains("cadastroTurmas")){
					logger.info("Login verificado com sucesso - acesso à página de turmas confirmado");
					return true;
				}
				logger.error("Acesso negado - usuário não autenticado ou credenciais inválidas");
            return false;
        }
    }
    
    private DadosIniciais obterDadosIniciais() throws IOException {
        Request getPageRequest = new Request.Builder()
                .url(TURMAS_URL)
                .header("User-Agent", USER_AGENT)
                .build();
        
        try (Response pageResponse = httpClient.newCall(getPageRequest).execute()) {
            if (!pageResponse.isSuccessful()) {
                throw new IOException("Erro ao carregar página inicial: " + pageResponse.code());
            }
            
            String html = pageResponse.body().string();
            Document doc = Jsoup.parse(html);
            
            DadosIniciais dados = new DadosIniciais();
            
            // Capturar semestre atual (opção selecionada no dropdown)
            Element semestreSelect = doc.select("select#formBusca\\:selectSemestre").first();
            if (semestreSelect != null) {
                Element selecionado = semestreSelect.select("option[selected]").first();
                if (selecionado != null) {
                    dados.setSemestreAtual(selecionado.attr("value"));
                    logger.info("Semestre atual detectado: {}", dados.getSemestreAtual());
                }
            }
            
            // Capturar centros disponíveis (dropdown de campus)
            Element centroSelect = doc.select("select#formBusca\\:selectCampus").first();
            if (centroSelect != null) {
                Elements opcoesCentros = centroSelect.select("option");
                HashMap<String, String> centros = new HashMap<>();
                
                for (Element opcao : opcoesCentros) {
                    String id = opcao.attr("value");
                    String nome = opcao.text().trim();
                    
                    if (!id.isEmpty() && !nome.isEmpty()) {
                        centros.put(id, nome);
                        // logger.debug("Centro encontrado: {} -> {}", id, nome);
                    }
                }
                dados.setCentros(centros);
                logger.info("Total de centros encontrados: {}", centros.size());
            }
            
            return dados;
        }
    }
    
    /**
     * Gera os últimos 7 semestres a partir de um semestre específico
     */
    private ArrayList<String> gerarSemestres(String semestreAtual) {
        ArrayList<String> semestres = new ArrayList<>();
        
        if (semestreAtual == null || semestreAtual.length() != 5) {
            logger.warn("Semestre atual inválido: {}. Usando semestre padrão.", semestreAtual);
            semestreAtual = "20261"; // fallback
        }
        
        try {
            // Extrair ano e período do semestre atual (formato: AAAAP)
            int ano = Integer.parseInt(semestreAtual.substring(0, 4));
            int periodo = Integer.parseInt(semestreAtual.substring(4));
            
            // Adicionar semestre atual
            semestres.add(semestreAtual);
            
            // Gerar 6 semestres anteriores
            for (int i = 1; i < 7; i++) {
                periodo--;
                if (periodo < 1) {
                    periodo = 3; // Volta para período 3 do ano anterior
                    ano--;
                }
                
                String semestre = String.format("%04d%d", ano, periodo);
                semestres.add(semestre);
            }
            
        } catch (NumberFormatException e) {
            logger.error("Erro ao processar semestre atual: {}. Usando lista padrão.", semestreAtual);
            // Lista padrão como fallback
            semestres.clear();
            semestres.add("20261");
            semestres.add("20253");
            semestres.add("20252");
            semestres.add("20251");
            semestres.add("20243");
            semestres.add("20242");
            semestres.add("20241");
        }
        
        return semestres;
    }
    
    private Document efetuarBusca(String semestre, String centro, ScrapingResult result) throws Exception {
		logger.debug("Processando: Semestre={}, Centro={}", semestre, centro);
		
		// Obter a página atual
		Request getPageRequest = new Request.Builder()
					.url(TURMAS_URL)
					.header("User-Agent", USER_AGENT)
					.build();
					
		try (Response pageResponse = httpClient.newCall(getPageRequest).execute()) {
			String html = pageResponse.body().string();
			Document doc = Jsoup.parse(html);
			
			// Encontrar formulário e dados necessários
			Element form = doc.select("form").first();
			String actionUrl = form.attr("action");
			if (actionUrl.startsWith("/")) {
					actionUrl = "https://cagr.sistemas.ufsc.br" + actionUrl;
			}
			
			// Extrair ID do botão de busca dinamicamente
			String botaoBuscarId = extrairIdBotaoBusca(doc);
			// logger.debug("Botao de busca encontrado = {}", botaoBuscarId);
			
			// ÚNICA REQUISIÇÃO: Submeter busca diretamente
			FormBody.Builder formBuilder = new FormBody.Builder();
			
			// Campos ocultos necessários
			Elements hiddenInputs = doc.select("input[type=hidden]");
			for (Element input : hiddenInputs) {
					String name = input.attr("name");
					String value = input.attr("value");
					if (!name.isEmpty()) {
						formBuilder.add(name, value);
					}
			}
			
			// Campos do formulário
			formBuilder.add("formBusca:selectSemestre", semestre);
			formBuilder.add("formBusca:selectCampus", centro);
			formBuilder.add("formBusca:selectCurso", "0"); // "Todos os cursos"
			formBuilder.add(botaoBuscarId, botaoBuscarId);
			
			RequestBody formBody = formBuilder.build();
			
			// Fazer a requisição de busca
			Request searchRequest = new Request.Builder()
						.url(actionUrl)
						.post(formBody)
						.addHeader("Content-Type", "application/x-www-form-urlencoded")
						.addHeader("User-Agent", USER_AGENT)
						.build();
						
			try (Response searchResponse = httpClient.newCall(searchRequest).execute()) {
					if (!searchResponse.isSuccessful()) {
						logger.warn("Erro ao buscar disciplinas para centro {}, semestre {}: {}", centro, semestre, searchResponse.code());
						return null;
					}
					String resultHtml = searchResponse.body().string();
					Document resultDoc = Jsoup.parse(resultHtml);
					return resultDoc;
			}
		}
	}

	private String extrairIdBotaoBusca(Document doc) {
		Elements botoesBuscar = doc.select("input[type=submit][onclick*='formBusca'], button[onclick*='formBusca']");
		
		for (Element botao : botoesBuscar) {
			String id = botao.attr("id");
			String name = botao.attr("name");
			if (id.contains("formBusca") || name.contains("formBusca")) {
					String botaoBuscarId = !id.isEmpty() ? id : name;
					// logger.debug("ID do botão de busca encontrado: {}", botaoBuscarId);
					return botaoBuscarId;
			}
		}
		
		logger.warn("Botão de busca não encontrado, usando ID padrão");
		return "formBusca:j_id164"; // fallback
	}
    
    private void processarPaginas(Document doc, String semestre, String centro, ScrapingResult result) throws Exception {
		int paginaAtual = 1;
		Document currentDoc = doc;

		while(currentDoc != null){
			logger.debug("Processando página {} para semestre={}, centro={}", paginaAtual, semestre, centro);

			processarTabelaPagina(currentDoc, semestre, centro, result);
			logger.info("total de {} disciplinas processadas até a página {}", result.getNumDisciplinasSalvas(), paginaAtual);

			Element nextButton = temProximaPagina(currentDoc);
			if(nextButton != null) {
				currentDoc = proximaPagina(nextButton, currentDoc, semestre, centro);
				paginaAtual++;
			} else {
				logger.debug("Ultima página alcançada para o semestre={}, centro={}", semestre, centro);
				break;
			}
		}
    }

	 private void processarTabelaPagina(Document doc, String semestre, String centro, ScrapingResult result){
		Element tabela = doc.select("table[id='formBusca:dataTable']").first();
        if (tabela == null) {
            logger.warn("Nenhuma tabela encontrada para semestre={}, centro={}", semestre, centro);
            return;
        }
        
        Elements linhas = tabela.select("tr");
        // logger.debug("Quantidade de linhas={}", linhas.size());
        
        for (int i = 1; i < linhas.size(); i++) { // Pular cabeçalho
            Element linha = linhas.get(i);
            Elements colunas = linha.select("td");
            // logger.debug("Quantidade de colunas={}", colunas.size());
            if (colunas.size() == 14) { // Verificar se tem todas as 14 colunas
                try {
						 // Criar elemento artificial para usar método existente
						 Element linhaElement = linha;
						 DisciplinaInfo info = extrairInformacoesLinha(linhaElement);
						//  logger.debug("Disciplina encontrada: {}", info.getNome());
                    if (info != null && !info.getCodigo().isEmpty()) {
							  Set<Professor> professores = new HashSet<>();
								for(ProfessorInfo professorInfo: info.getProfessores()){
									// logger.debug("Processando professor: {} ({})", professorInfo.getNome(), professorInfo.getLattesId());
									try{
										Professor professor = professorService.criarOuObter(professorInfo.getLattesId(), professorInfo.getNome());
										professores.add(professor);
										result.addProfessor(professor);
									} catch(Exception e){
										logger.warn("Erro ao criar/obter professor {}: {}", professorInfo.getNome(), e.getMessage());
										throw new RuntimeException(e);
									}
								}
								try{
									Disciplina disciplina = disciplinaService.criarOuAtualizar(info.getCodigo(), info.getNome(), professores, semestre);
									logger.debug("Disciplina processada: {} - {}", disciplina.getCodigo(), disciplina.getNome());
									result.addDisciplina(disciplina);
                                    // logger.debug("Disciplina criada/atualizada com sucesso: {}", disciplina.toString());
								} catch(Exception e){
									logger.warn("Erro ao criar/atualizar disciplina {}: {}", info.getNome(),
											e.getMessage());
									Thread.sleep(60000);
									throw new RuntimeException(e);
								}

                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar linha da tabela: {}", e.getMessage());
                }
            }
        }
	 }
    
    private DisciplinaInfo extrairInformacoesLinha(Element linha) {
        Elements celulas = linha.select("td");
        
        DisciplinaInfo info = new DisciplinaInfo();
        
        try {
            // Extrair código da disciplina (coluna 3)
            String codigo = celulas.get(3).text().trim();
            if (codigo.isEmpty()) {
                return null; // Código inválido
            }
            info.setCodigo(codigo);
            
            // Extrair nome da disciplina (coluna 5)
            String nome = celulas.get(5).text().trim();
            if (nome.isEmpty()) {
                return null;
            }
            info.setNome(nome);
            
            // Extrair professores (coluna 13) - pode haver múltiplos
            Element celulaProfessor = celulas.get(13);
            Elements linksProfessores = celulaProfessor.select("a[href*=lattes]");

            Set<ProfessorInfo> professores = new HashSet<>();

            for (Element linkProfessor : linksProfessores) {
                String nomeProfessor = linkProfessor.text().trim();
                String href = linkProfessor.attr("href");
                String lattesId = extrairLattesId(href);
                
                if (!nomeProfessor.isEmpty() && lattesId != null) {
                    ProfessorInfo profInfo = new ProfessorInfo();
                    profInfo.setNome(nomeProfessor);
                    profInfo.setLattesId(lattesId);
                    professores.add(profInfo);
                }
            }
            
            info.setProfessores(professores);
            
        } catch (Exception e) {
            logger.warn("Erro ao extrair informações da linha: {}", e.getMessage());
            return null;
        }
        
        return info;
    }
    
    private String extrairLattesId(String href) {
        if (href == null || href.isEmpty()) {
            return null;
        }
        
        // Padrões comuns para ID Lattes
        Pattern[] patterns = {
            Pattern.compile("id=([0-9]+)"),
            Pattern.compile("lattes/([0-9]+)"),
            Pattern.compile("([0-9]{16})") // ID Lattes tem 16 dígitos
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(href);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
	 private Element temProximaPagina(Document doc){
		Elements nextButtons = doc.select("td.rich-datascr-button[onclick*='next']");

		for (Element control : nextButtons) {
			String onclick = control.attr("onclick");
			String className = control.attr("class");
			
			// Verificar se é o botão "next" e se está habilitado
			if (onclick.contains("'next'") && !className.contains("rich-datascr-button-dsbld")) {
				return control;
			}
		}
		
		// logger.debug("Próxima página não disponível");
		return null;
	 }

	 private Document proximaPagina(Element nextButton, Document currentDoc, String semestre, String centro) throws Exception {
		// Encontrar o formulário
		Element form = currentDoc.select("form#formBusca").first();
		if (form == null) {
			logger.warn("Formulário não encontrado para navegação");
			return null;
		}
		
		String actionUrl = form.attr("action");
		if (actionUrl.startsWith("/")) {
			actionUrl = "https://cagr.sistemas.ufsc.br" + actionUrl;
		}
		
		// Construir a requisição AJAX para o DataScroller
		FormBody.Builder formBuilder = new FormBody.Builder();
		
		// Adicionar todos os campos ocultos da página atual
		Elements hiddenInputs = currentDoc.select("input[type=hidden]");
		for (Element input : hiddenInputs) {
			String name = input.attr("name");
			String value = input.attr("value");
			if (!name.isEmpty()) {
				formBuilder.add(name, value);
			}
		}
	
		// Adicionar campos específicos do form de busca para manter o contexto
		Elements searchInputs = currentDoc.select("#formBusca input, #formBusca select");
    	for (Element input : searchInputs) {
        String name = input.attr("name");
        String value = input.attr("value");
        String tagName = input.tagName().toLowerCase();
        
        if (!name.isEmpty() && !name.equals("javax.faces.ViewState")) {
            if (tagName.equals("select")) {
                // Para selects, pegar a opção selecionada
                Element selectedOption = input.select("option[selected]").first();
                if (selectedOption != null) {
                    value = selectedOption.attr("value");
                }
            }
            
            if (value != null && !value.isEmpty()) {
                formBuilder.add(name, value);
               //  logger.debug("Campo mantido: {} = {}", name, value);
            }
        }
    }
    
    // Parâmetros específicos do DataScroller (baseado no HTML)
    formBuilder.add("formBusca:dataScroller1", "next");
    formBuilder.add("autoScroll", "");
    
    // Headers específicos para requisição AJAX do RichFaces
    Request nextPageRequest = new Request.Builder()
            .url(actionUrl)
            .post(formBuilder.build())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Faces-Request", "partial/ajax")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build();
    
    logger.debug("Enviando requisição AJAX para DataScroller...");
    
    try (Response response = httpClient.newCall(nextPageRequest).execute()) {
      //   logger.debug("Resposta recebida. Status: {}", response.code());
        
        if (!response.isSuccessful()) {
            logger.error("Erro na requisição: {}", response.code());
            return null;
        }
        
        String responseBody = response.body().string();
      //   logger.debug("Resposta recebida. Tamanho: {} chars", responseBody.length());
        
        // Para RichFaces, a resposta pode ser XML com updates parciais
        // Vamos tentar primeiro como HTML normal
        Document newDoc = Jsoup.parse(responseBody);
        
        // Verificar se temos uma tabela de dados na resposta
        Elements newTable = newDoc.select("table#formBusca\\:dataTable tr");
		  if (newTable.size() > 1) {
				// logger.debug("Nova página carregada com {} linhas", newTable.size());
				
				// Verificar se o conteúdo realmente mudou
				Elements oldTable = currentDoc.select("table#formBusca\\:dataTable tr");
				if (oldTable.size() > 1 && newTable.size() > 1) {
						String primeiraLinhaAntiga = oldTable.get(1).text();
						String primeiraLinhaNova = newTable.get(1).text();
						
						boolean mudou = !primeiraLinhaAntiga.equals(primeiraLinhaNova);
						// logger.debug("Página mudou: {}", mudou);
						
						if (!mudou) {
							logger.warn("ATENÇÃO: A página parece não ter mudado!");
						}
				}
				return newDoc;
        } else {
            // Se não encontrou tabela, pode ser uma resposta XML do RichFaces
            // logger.debug("Resposta não contém tabela HTML direta, pode ser resposta AJAX XML");
            
            // Tentar extrair HTML da resposta XML se necessário
            if (responseBody.contains("formBusca:dataTable")) {
                // A resposta contém dados da tabela, mas em formato XML
                // Vamos tentar uma nova requisição para obter a página completa
               //  logger.debug("Fazendo nova requisição para obter página completa...");
                
                Request fullPageRequest = new Request.Builder()
                        .url(actionUrl)
                        .get()
                        .addHeader("User-Agent", USER_AGENT)
                        .build();
                
                try (Response fullPageResponse = httpClient.newCall(fullPageRequest).execute()) {
                    if (fullPageResponse.isSuccessful()) {
                        Document fullDoc = Jsoup.parse(fullPageResponse.body().string());
                        Elements fullTable = fullDoc.select("table#formBusca\\:dataTable tr");
                        
                        if (fullTable.size() > 1) {
                           //  logger.debug("Página completa obtida com {} linhas", fullTable.size());
                            return fullDoc;
                        }
                    }
                }
            }
		  }   
			logger.warn("Não foi possível obter nova página válida");
			return null;
    	}
	 }
}