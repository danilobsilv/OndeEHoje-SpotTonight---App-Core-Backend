package com.spotnight.category.service;

import com.spotnight.category.dto.CategoryResponse;
import com.spotnight.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categories.findByActiveTrueOrderByNameAscSlugAsc().stream().map(CategoryResponse::from).toList();
    }
}
