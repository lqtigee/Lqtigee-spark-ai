import type { SourceCapabilityDto } from "../types/api";

interface CodexOptionsSheetProps {
  capability: SourceCapabilityDto;
}

export function CodexOptionsSheet({ capability }: CodexOptionsSheetProps) {
  return (
    <section className="options-sheet" aria-label="Codex 能力">
      <h4>Codex</h4>
      <CapabilityGroup
        title="运行选项"
        capabilities={capability.runOptions}
        labels={{
          model: "模型"
        }}
      />
      <CapabilityGroup
        title="附件"
        capabilities={capability.attachments}
        labels={{
          image: "图片附件"
        }}
      />
      <CapabilityGroup
        title="危险选项"
        capabilities={capability.dangerousOptions}
        labels={{}}
      />
    </section>
  );
}

function CapabilityGroup({ title, capabilities, labels }: { title: string; capabilities: string[]; labels: Record<string, string> }) {
  const knownCapabilities = capabilities.filter((capability) => labels[capability]);
  if (knownCapabilities.length === 0) {
    return null;
  }

  return (
    <div className="options-sheet__group">
      <span>{title}</span>
      <ul className="options-sheet__chips">
        {knownCapabilities.map((capability) => (
          <li key={capability}>{labels[capability]}</li>
        ))}
      </ul>
    </div>
  );
}
