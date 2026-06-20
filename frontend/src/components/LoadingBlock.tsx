interface LoadingBlockProps {
  label: string;
}

export function LoadingBlock({ label }: LoadingBlockProps) {
  return (
    <section aria-busy="true" aria-live="polite">
      <p>{label}</p>
    </section>
  );
}
