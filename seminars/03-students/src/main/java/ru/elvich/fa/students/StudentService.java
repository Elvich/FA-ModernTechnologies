package ru.elvich.fa.students;

import java.util.List;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final EntityManager entityManager;

    public List<StudentEntity> findAll() {
        return studentRepository.findAll(Sort.by("id"));
    }

    public StudentEntity findById(Long id) {
        return studentRepository.findById(id).map(student -> {
            log.info("Student with id={} found", id);
            return student;
        }).orElseThrow(() -> {
            log.warn("Student with id={} not found", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Студент не найден");
        });
    }

    @Transactional
    public StudentEntity save(StudentEntity student) {
        return studentRepository.saveAndFlush(student);
    }

    @Transactional
    public StudentEntity create(StudentEntity student) {
        if (studentRepository.existsById(student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой ID уже существует");
        }
        entityManager.persist(student);
        entityManager.flush();
        return student;
    }

    @Transactional
    public StudentEntity update(Long id, StudentUpdate input) {
        StudentEntity student = findById(id);
        student.setFirstName(input.firstName());
        student.setLastName(input.lastName());
        return save(student);
    }

    @Transactional
    public void deleteById(Long id) {
        studentRepository.delete(findById(id));
    }
}
