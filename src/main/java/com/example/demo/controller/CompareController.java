package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/compare")
@SessionAttributes("compareList")
public class CompareController {

    @Autowired
    private ProductRepository productRepository;

    @ModelAttribute("compareList")
    public List<String> compareList() {
        return new ArrayList<>();
    }

    @GetMapping
    public String showCompare(
            @SessionAttribute(value = "compareList", required = false) List<String> compareList,
            Model model) {
        
        List<Product> products = new ArrayList<>();
        if (compareList != null) {
            for (String slug : compareList) {
                productRepository.findBySlug(slug).ifPresent(products::add);
            }
        }
        
        model.addAttribute("products", products);
        model.addAttribute("maxCompare", 4);
        return "compare";
    }

    @PostMapping("/add")
    public String addToCompare(
            @RequestParam String slug,
            @SessionAttribute(value = "compareList", required = false) List<String> compareList,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        
        List<String> list = compareList;
        if (list == null) {
            list = new ArrayList<>();
        }

        if (list.contains(slug)) {
            redirectAttrs.addFlashAttribute("message", "Sản phẩm này đã có trong danh sách so sánh!");
        } else if (list.size() >= 4) {
            redirectAttrs.addFlashAttribute("message", "Tối đa 4 sản phẩm để so sánh!");
        } else {
            list.add(slug);
            redirectAttrs.addFlashAttribute("message", "Đã thêm vào danh sách so sánh!");
        }
        
        redirectAttrs.addFlashAttribute("compareList", list);
        return "redirect:/compare";
    }

    @PostMapping("/remove")
    public String removeFromCompare(
            @RequestParam String slug,
            @ModelAttribute("compareList") List<String> compareList,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        
        compareList.remove(slug);
        redirectAttrs.addFlashAttribute("message", "Đã xóa khỏi danh sách so sánh!");
        return "redirect:/compare";
    }

    @PostMapping("/clear")
    public String clearCompare(
            @ModelAttribute("compareList") List<String> compareList,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        
        compareList.clear();
        redirectAttrs.addFlashAttribute("message", "Đã xóa tất cả!");
        return "redirect:/compare";
    }

    @GetMapping("/count")
    @ResponseBody
    public int getCompareCount(
            @SessionAttribute(value = "compareList", required = false) List<String> compareList) {
        return compareList != null ? compareList.size() : 0;
    }
}
