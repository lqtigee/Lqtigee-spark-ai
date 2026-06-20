import { AppShell } from "../components/AppShell";
import { ControlPage } from "../pages/ControlPage";
import { OverviewPage } from "../pages/OverviewPage";
import { RunsPage } from "../pages/RunsPage";
import { SessionsPage } from "../pages/SessionsPage";
import { SettingsPage } from "../pages/SettingsPage";

type PageComponent = () => React.JSX.Element;

export function App() {
  const Page = resolvePage(window.location.pathname);

  return (
    <AppShell>
      <Page />
    </AppShell>
  );
}

function resolvePage(pathname: string): PageComponent {
  switch (pathname) {
    case "/":
      return OverviewPage;
    case "/sessions":
      return SessionsPage;
    case "/control":
      return ControlPage;
    case "/runs":
      return RunsPage;
    case "/settings":
      return SettingsPage;
    default:
      return OverviewPage;
  }
}
