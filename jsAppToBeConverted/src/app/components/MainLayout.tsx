import { Outlet, useLocation, useNavigate } from "react-router";
import { Home, Calendar, Award, User, Sparkles } from "lucide-react";

export function MainLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const tabs = [
    { id: "home", label: "Início", icon: Home, path: "/app" },
    { id: "booking", label: "Marcar", icon: Sparkles, path: "/app/booking" },
    { id: "bookings", label: "Marcações", icon: Calendar, path: "/app/bookings" },
    { id: "rewards", label: "Recompensas", icon: Award, path: "/app/rewards" },
    { id: "profile", label: "Perfil", icon: User, path: "/app/profile" },
  ];

  const isActive = (path: string) => {
    if (path === "/app") {
      return location.pathname === "/app";
    }
    return location.pathname.startsWith(path);
  };

  return (
    <div className="flex flex-col h-screen bg-[#f8f9fa] max-w-md mx-auto relative">
      {/* Main Content */}
      <div className="flex-1 overflow-y-auto pb-20">
        <Outlet />
      </div>

      {/* Bottom Navigation */}
      <div className="fixed bottom-0 left-0 right-0 max-w-md mx-auto bg-white border-t border-gray-200 safe-area-inset-bottom">
        <nav className="flex items-center justify-around px-2 py-2">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const active = isActive(tab.path);

            return (
              <button
                key={tab.id}
                onClick={() => navigate(tab.path)}
                className={`flex flex-col items-center justify-center gap-1 px-3 py-2 rounded-xl transition-all ${
                  active
                    ? "text-[#0f1729]"
                    : "text-gray-400 hover:text-gray-600"
                }`}
              >
                <div
                  className={`p-1.5 rounded-lg transition-colors ${
                    active ? "bg-[#fbbf24]" : ""
                  }`}
                >
                  <Icon className="w-5 h-5" />
                </div>
                <span className="text-xs">{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>
    </div>
  );
}
