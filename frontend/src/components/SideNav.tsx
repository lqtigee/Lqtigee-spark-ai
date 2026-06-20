const NAV_LINKS = [
  { label: "Overview", href: "/" },
  { label: "Sessions", href: "/sessions" },
  { label: "Control", href: "/control" },
  { label: "Runs", href: "/runs" },
  { label: "Settings", href: "/settings" }
];

export function SideNav() {
  const pathname = window.location.pathname;

  return (
    <nav aria-label="Side navigation">
      <ul>
        {NAV_LINKS.map((link) => (
          <li key={link.href}>
            <a aria-current={pathname === link.href ? "page" : undefined} href={link.href}>
              {link.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
