package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.dto.CodexSkillDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CodexSkillService {

    private static final String SKILL_FILE_NAME = "SKILL.md";
    private static final int MAX_METADATA_LINES = 80;
    private static final int MAX_DESCRIPTION_CHARS = 240;

    private final List<Path> skillRoots;

    public CodexSkillService() {
        this(defaultSkillRoots());
    }

    CodexSkillService(List<Path> skillRoots) {
        this.skillRoots = skillRoots.stream()
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .toList();
    }

    public List<CodexSkillDto> listSkills() {
        List<Path> readableRoots = readableRoots();
        List<CodexSkillDto> skills = new ArrayList<>();
        for (Path root : readableRoots) {
            skills.addAll(scanRoot(root));
        }
        return skills.stream()
                .sorted(Comparator.comparing(CodexSkillDto::name).thenComparing(CodexSkillDto::sourcePath))
                .toList();
    }

    private List<CodexSkillDto> scanRoot(Path root) {
        try (Stream<Path> paths = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> SKILL_FILE_NAME.equals(path.getFileName().toString()))
                    .map(path -> toSkill(path.toAbsolutePath().normalize()))
                    .toList();
        } catch (IOException exception) {
            throw skillScanFailed("Codex skill scan failed", exception.getMessage());
        }
    }

    private CodexSkillDto toSkill(Path skillFile) {
        SkillMetadata metadata = readMetadata(skillFile);
        String sourcePath = skillFile.toString();
        return new CodexSkillDto(
                metadata.name() + "|" + sourcePath,
                metadata.name(),
                sourcePath,
                metadata.description()
        );
    }

    private SkillMetadata readMetadata(Path skillFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(skillFile);
        } catch (IOException exception) {
            throw skillScanFailed("Codex skill metadata read failed", exception.getMessage());
        }

        if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) {
            throw skillScanFailed("Codex skill metadata is missing", skillFile.toString());
        }

        String name = "";
        String description = "";
        int limit = Math.min(lines.size(), MAX_METADATA_LINES);
        for (int index = 1; index < limit; index++) {
            String line = lines.get(index).trim();
            if ("---".equals(line)) {
                return requireMetadata(skillFile, name, description);
            }
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String value = unquote(line.substring(separatorIndex + 1).trim());
            if ("name".equals(key)) {
                name = value;
            } else if ("description".equals(key)) {
                description = abbreviate(value);
            }
        }

        throw skillScanFailed("Codex skill metadata terminator is missing", skillFile.toString());
    }

    private SkillMetadata requireMetadata(Path skillFile, String name, String description) {
        if (name == null || name.isBlank()) {
            throw skillScanFailed("Codex skill name is missing", skillFile.toString());
        }
        if (description == null || description.isBlank()) {
            throw skillScanFailed("Codex skill description is missing", skillFile.toString());
        }
        return new SkillMetadata(name.trim(), description.trim());
    }

    private List<Path> readableRoots() {
        if (skillRoots.isEmpty()) {
            throw new ApiException(
                    ErrorCode.CODEX_HOME_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex skill roots are not configured",
                    "skillRoots=[]"
            );
        }

        List<Path> readableRoots = new ArrayList<>();
        for (Path root : skillRoots) {
            if (!Files.exists(root)) {
                continue;
            }
            if (!Files.isDirectory(root) || !Files.isReadable(root)) {
                throw skillScanFailed("Codex skill root is not readable", root.toString());
            }
            readableRoots.add(root);
        }

        if (readableRoots.isEmpty()) {
            throw new ApiException(
                    ErrorCode.CODEX_HOME_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex skill directories not found",
                    skillRoots.toString()
            );
        }
        return List.copyOf(readableRoots);
    }

    private static List<Path> defaultSkillRoots() {
        Path home = Path.of(System.getProperty("user.home"));
        return List.of(
                home.resolve(".codex").resolve("skills"),
                home.resolve(".agents").resolve("skills"),
                Path.of("/etc/codex/skills")
        );
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String abbreviate(String value) {
        if (value.length() <= MAX_DESCRIPTION_CHARS) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_CHARS).trim();
    }

    private ApiException skillScanFailed(String message, String detail) {
        return new ApiException(
                ErrorCode.CODEX_SESSION_SCAN_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                message,
                detail
        );
    }

    private record SkillMetadata(String name, String description) {
    }
}
