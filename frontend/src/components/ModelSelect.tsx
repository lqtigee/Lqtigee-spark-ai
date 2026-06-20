import type { AgentSource, ModelDto } from "../types/api";

interface ModelSelectProps {
  models: ModelDto[];
  value: string;
  onChange(value: string): void;
  source: AgentSource;
}

export function ModelSelect({ models, value, onChange, source }: ModelSelectProps) {
  const availableModels = models.filter((model) => model.enabled && model.sources.includes(source));

  return (
    <label className="field">
      <span>Model</span>
      <select
        className="input-control"
        disabled={availableModels.length === 0}
        onChange={(event) => onChange(event.target.value)}
        value={availableModels.length === 0 ? "" : value}
      >
        {availableModels.length === 0 ? <option value="">No models available</option> : null}
        {availableModels.length > 0 ? <option value="">Select model</option> : null}
        {availableModels.map((model) => (
          <option key={model.id} value={model.id}>
            {model.label}
          </option>
        ))}
      </select>
    </label>
  );
}
