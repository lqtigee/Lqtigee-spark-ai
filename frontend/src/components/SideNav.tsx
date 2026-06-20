const NAV_LINKS = [
  { label: "首页", href: "/" },
  { label: "会话", href: "/sessions" },
  { label: "控制", href: "/control" },
  { label: "运行", href: "/runs" },
  { label: "设置", href: "/settings" }
];

export function SideNav() {
  const pathname = window.location.pathname;

  return (
    <nav aria-label="侧边导航">
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
