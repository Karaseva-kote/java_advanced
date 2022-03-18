package info.kgeorgiy.ja.karaseva.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private final Comparator<Student> comparatorByName = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparingInt(Student::getId);

    private <T, collection extends Collection<T>> collection collectStudents(List<Student> students,
                                                                             Function<Student, T> mapper,
                                                                             Collector<T, ?, collection> collector) {
        return students.stream().map(mapper).collect(collector);
    }

    private <T> List<T> listStudents(List<Student> students, Function<Student, T> mapper) {
        return collectStudents(students, mapper, Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return listStudents(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return listStudents(students,Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return listStudents(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return listStudents(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return collectStudents(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> cmp) {
        return students.stream().sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, comparatorByName);
    }

    private List<Student> filterStudents(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortStudentsByName(filterStudents(students, student -> student.getFirstName().equals(name)));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortStudentsByName(filterStudents(students, student -> student.getLastName().equals(name)));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortStudentsByName(filterStudents(students, student -> student.getGroup().equals(group)));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo))
                );
    }
}
