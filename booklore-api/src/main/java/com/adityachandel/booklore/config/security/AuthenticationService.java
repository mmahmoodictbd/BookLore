package com.adityachandel.booklore.config.security;

import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.entity.EpubViewerPreferencesEntity;
import com.adityachandel.booklore.repository.PdfViewerPreferencesRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AuthenticationService {

    public BookLoreUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (BookLoreUser) authentication.getPrincipal();
    }
}