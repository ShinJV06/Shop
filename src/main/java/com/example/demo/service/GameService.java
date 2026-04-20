package com.example.demo.service;

import com.example.demo.entity.Game;
import com.example.demo.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    public List<Game> getAllVisibleGames() {
        return gameRepository.findAllVisibleOrderByDisplayOrder();
    }

    public Optional<Game> getGameBySlug(String slug) {
        return gameRepository.findBySlugIgnoreCase(slug);
    }

    public Optional<Game> findBySlug(String slug) {
        return gameRepository.findBySlug(slug);
    }

    public Game save(Game game) {
        return gameRepository.save(game);
    }

    public void deleteById(Long id) {
        gameRepository.deleteById(id);
    }
}
