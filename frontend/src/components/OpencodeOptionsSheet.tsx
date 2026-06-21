import type { SourceCapabilityDto } from "../types/api";

interface OpencodeOptionsSheetProps {
  capability: SourceCapabilityDto;
}

export function OpencodeOptionsSheet({ capability }: OpencodeOptionsSheetProps) {
  return (
    <section className="options-sheet" aria-label="opencode 能力">
      <h4>opencode</h4>
      <CapabilityGroup
        title="运行选项"
        capabilities={capability.runOptions}
        labels={{
          model: "模型",
          agent: "Agent",
          fork: "Fork",
          share: "Share",
          variant: "Variant",
          thinking: "Thinking",
          replay: "Replay",
          replayLimit: "Replay 限制"
        }}
      />
      <CapabilityGroup
        title="附件"
        capabilities={capability.attachments}
        labels={{
          file: "文件附件"
        }}
      />
      <CapabilityGroup
        title="危险选项"
        capabilities={capability.dangerousOptions}
        labels={{
          shellDangerouslySkipPermissions: "跳过权限确认"
        }}
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
