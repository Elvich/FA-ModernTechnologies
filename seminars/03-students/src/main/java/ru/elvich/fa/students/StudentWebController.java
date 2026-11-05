package ru.elvich.fa.students;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StudentWebController {
    private final StudentService studentService;

    @GetMapping("/")
    public String index() {
        return "redirect:/web/students";
    }

    @GetMapping("/web/students")
    public String students(Model model) {
        model.addAttribute("students", studentService.findAll());
        return "students";
    }
}
