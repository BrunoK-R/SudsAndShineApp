import { Home, Calendar, History, Gift, User } from "lucide-react";
import { useNavigate, useLocation } from "react-router";

export function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { path: "/home", label: "Início", icon: Home },
    { path: "/booking/service", label: "Marcar", icon: Calendar },
    { path: "/bookings", label: "Marcações", icon: History },
    { path: "/loyalty", label: "Recompensas", icon: Gift },
    { path: "/profile", label: "Perfil", icon: User },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50 safe-bottom">
      <div className="max-w-md mx-auto flex items-center justify-around h-20">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);
          
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className="flex flex-col items-center justify-center flex-1 h-full gap-1"
            >
              <Icon
                className={`w-6 h-6 ${
                  isActive ? "text-[#D4AF37]" : "text-gray-400"
                }`}
              />
              <span
                className={`text-xs ${
                  isActive
                    ? "text-[#0A1929] font-medium"
                    : "text-gray-500"
                }`}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
