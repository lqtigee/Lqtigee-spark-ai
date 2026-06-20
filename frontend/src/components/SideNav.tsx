const NAV_LINKS = [
  { label: "Home", href: "/" },
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
            <a className="nav-link" aria-current={pathname === link.href ? "page" : undefined} href={link.href}>
              {link.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
