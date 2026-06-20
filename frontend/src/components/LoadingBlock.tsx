interface LoadingBlockProps {
  label: string;
}

export function LoadingBlock({ label }: LoadingBlockProps) {
  return (
    <div className="loading-block" aria-busy="true" aria-live="polite">
      <span className="loading-block__bar" />
      <p>{label}</p>
    </div>
  );
}
