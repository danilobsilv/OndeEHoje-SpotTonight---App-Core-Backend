package com.ondeehoje.category.controller;

import com.ondeehoje.category.dto.CategoryResponse;
import com.ondeehoje.category.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categories.listActive();
    }
}
