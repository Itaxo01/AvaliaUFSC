package com.example.controller;

import com.example.model.Disciplina;
import com.example.model.ScrapperStatus;
import com.example.service.DisciplinaService;
import com.example.service.ScrapperStatusService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.example.DTO.DisciplinaSearchDTO;

/* Só vai fazer uma coisa, retornar as disciplinas ao front-end ao selecionar a barra de pesquisa */
@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    @Autowired
    private DisciplinaService disciplinaService;
    
    @Autowired
    private ScrapperStatusService scrapperStatusService;
    
	 @Transactional
    @GetMapping("/disciplinas")
    public ResponseEntity<List<DisciplinaSearchDTO>> searchDisciplinas() {
        List<Disciplina> resultados = disciplinaService.buscarTodas();
		  List<DisciplinaSearchDTO> dtoList = resultados.stream()
				.map(d -> DisciplinaSearchDTO.from(d))
				.toList();
        return ResponseEntity.ok(dtoList);
    }
    
    /**
     * Returns the expected count of disciplinas from last successful scrape
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getExpectedCount() {
        ScrapperStatus status = scrapperStatusService.getUltimoStatus();
        int count = status.getDisciplinasCapturadas();
        
        return ResponseEntity.ok(count);
    }

    /**
     * Returns the data hash from the last successful scrape.
     * Used by frontend to check if cached data needs to be refreshed.
     */
    @GetMapping("/data-hash")
    public ResponseEntity<String> getDataHash() {
        ScrapperStatus status = scrapperStatusService.getUltimoStatus();
        String hash = status.getDataHash();
        
        // Se não existe hash, gera um novo baseado nos dados atuais
        if (hash == null || hash.isEmpty()) {
            hash = status.gerarDataHash();
        }
        
        return ResponseEntity.ok(hash);
    }
}
