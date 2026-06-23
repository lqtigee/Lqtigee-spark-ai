package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.CodexSkillDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexSkillServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void listSkillsReturnsSafeMetadataOnly() throws IOException {
        Path skillFile = writeSkill(
                tempDir.resolve("skills").resolve(".system").resolve("openai-docs"),
                "openai-docs",
                "Use when Codex needs official OpenAI documentation.",
                "Full private workflow instructions must stay server-side."
        );
        CodexSkillService service = new CodexSkillService(List.of(tempDir.resolve("skills")));

        List<CodexSkillDto> skills = service.listSkills();

        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).id()).isEqualTo("openai-docs|" + skillFile);
        assertThat(skills.get(0).name()).isEqualTo("openai-docs");
        assertThat(skills.get(0).sourcePath()).isEqualTo(skillFile.toString());
        assertThat(skills.get(0).description()).isEqualTo("Use when Codex needs official OpenAI documentation.");
        assertThat(skills.get(0).toString()).doesNotContain("Full private workflow instructions");
    }

    @Test
    void listSkillsReturnsEmptyWhenReadableRootContainsNoSkills() throws IOException {
        Path skillsRoot = Files.createDirectories(tempDir.resolve("skills"));
        CodexSkillService service = new CodexSkillService(List.of(skillsRoot));

        assertThat(service.listSkills()).isEmpty();
    }

    @Test
    void listSkillsFailsWhenNoSkillRootExists() {
        CodexSkillService service = new CodexSkillService(List.of(tempDir.resolve("missing-skills")));

        assertThatThrownBy(service::listSkills)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_HOME_NOT_FOUND));
    }

    @Test
    void listSkillsFailsWhenMetadataNameIsMissing() throws IOException {
        Path skillDir = Files.createDirectories(tempDir.resolve("skills").resolve("broken"));
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                description: "Missing name"
                ---
                Body
                """);
        CodexSkillService service = new CodexSkillService(List.of(tempDir.resolve("skills")));

        assertThatThrownBy(service::listSkills)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_SESSION_SCAN_FAILED));
    }

    private Path writeSkill(Path skillDir, String name, String description, String body) throws IOException {
        Files.createDirectories(skillDir);
        Path skillFile = skillDir.resolve("SKILL.md").toAbsolutePath().normalize();
        Files.writeString(skillFile, """
                ---
                name: "%s"
                description: "%s"
                ---
                
                %s
                """.formatted(name, description, body));
        return skillFile;
    }
}
