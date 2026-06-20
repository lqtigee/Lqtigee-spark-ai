import type { FormEvent } from "react";
import type { CommandMode } from "../types/api";

interface PromptComposerProps {
  prompt: string;
  mode: CommandMode;
  confirmDangerous: boolean;
  disabled: boolean;
  submitDisabled: boolean;
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
  submitDisabled,
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
    <form className="command-form" onSubmit={handleSubmit}>
      <fieldset className="mode-control">
        <legend>Mode</legend>
        <div className="mode-control__grid">
          {COMMAND_MODES.map((commandMode) => (
            <label className={mode === commandMode ? "mode-control__item mode-control__item--active" : "mode-control__item"} key={commandMode}>
              <input
                checked={mode === commandMode}
                disabled={disabled}
                onChange={() => onModeChange(commandMode)}
                type="radio"
                value={commandMode}
              />
              <span>{commandMode}</span>
            </label>
          ))}
        </div>
      </fieldset>
      <label className="field">
        <span>Prompt</span>
        <textarea className="input-control input-control--textarea" disabled={disabled} onChange={(event) => onPromptChange(event.target.value)} value={prompt} />
      </label>
      {mode === "SHELL" ? (
        <label className="check-row">
          <input
            checked={confirmDangerous}
            disabled={disabled}
            onChange={(event) => onConfirmDangerousChange(event.target.checked)}
            type="checkbox"
          />
          Confirm dangerous shell mode
        </label>
      ) : null}
      <button className="button button--primary button--wide" disabled={disabled || submitDisabled} type="submit">
        Run
      </button>
    </form>
  );
}
