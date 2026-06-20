import type { FormEvent } from "react";
import type { CommandMode } from "../types/api";

interface PromptComposerProps {
  prompt: string;
  mode: CommandMode;
  confirmDangerous: boolean;
  disabled: boolean;
  onPromptChange(prompt: string): void;
  onModeChange(mode: CommandMode): void;
  onConfirmDangerousChange(confirmDangerous: boolean): void;
  onSubmit(): void;
}

const COMMAND_MODES: CommandMode[] = ["ASK", "EDIT", "REVIEW", "SHELL"];

export function PromptComposer({
  prompt,
  mode,
  confirmDangerous,
  disabled,
  onPromptChange,
  onModeChange,
  onConfirmDangerousChange,
  onSubmit
}: PromptComposerProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        Mode
        <select
          disabled={disabled}
          onChange={(event) => onModeChange(event.target.value as CommandMode)}
          value={mode}
        >
          {COMMAND_MODES.map((commandMode) => (
            <option key={commandMode} value={commandMode}>
              {commandMode}
            </option>
          ))}
        </select>
      </label>
      <label>
        Prompt
        <textarea disabled={disabled} onChange={(event) => onPromptChange(event.target.value)} value={prompt} />
      </label>
      {mode === "SHELL" ? (
        <label>
          <input
            checked={confirmDangerous}
            disabled={disabled}
            onChange={(event) => onConfirmDangerousChange(event.target.checked)}
            type="checkbox"
          />
          Confirm dangerous shell mode
        </label>
      ) : null}
      <button disabled={disabled} type="submit">
        Run
      </button>
    </form>
  );
}
