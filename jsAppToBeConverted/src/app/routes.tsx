import { createBrowserRouter } from "react-router";
import SplashScreen from "./screens/SplashScreen";
import OnboardingScreen from "./screens/OnboardingScreen";
import LoginScreen from "./screens/LoginScreen";
import RegisterScreen from "./screens/RegisterScreen";
import ForgotPasswordScreen from "./screens/ForgotPasswordScreen";
import HomeScreen from "./screens/HomeScreen";
import ServicesScreen from "./screens/ServicesScreen";
import BookingServiceScreen from "./screens/BookingServiceScreen";
import BookingVehicleScreen from "./screens/BookingVehicleScreen";
import BookingDateTimeScreen from "./screens/BookingDateTimeScreen";
import BookingContactScreen from "./screens/BookingContactScreen";
import BookingConfirmationScreen from "./screens/BookingConfirmationScreen";
import BookingSuccessScreen from "./screens/BookingSuccessScreen";
import BookingsScreen from "./screens/BookingsScreen";
import HistoryScreen from "./screens/HistoryScreen";
import LoyaltyScreen from "./screens/LoyaltyScreen";
import RatingScreen from "./screens/RatingScreen";
import VehiclesScreen from "./screens/VehiclesScreen";
import ProfileScreen from "./screens/ProfileScreen";
import ContactScreen from "./screens/ContactScreen";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: SplashScreen,
  },
  {
    path: "/onboarding",
    Component: OnboardingScreen,
  },
  {
    path: "/login",
    Component: LoginScreen,
  },
  {
    path: "/register",
    Component: RegisterScreen,
  },
  {
    path: "/forgot-password",
    Component: ForgotPasswordScreen,
  },
  {
    path: "/home",
    Component: HomeScreen,
  },
  {
    path: "/services",
    Component: ServicesScreen,
  },
  {
    path: "/booking/service",
    Component: BookingServiceScreen,
  },
  {
    path: "/booking/vehicle",
    Component: BookingVehicleScreen,
  },
  {
    path: "/booking/datetime",
    Component: BookingDateTimeScreen,
  },
  {
    path: "/booking/contact",
    Component: BookingContactScreen,
  },
  {
    path: "/booking/confirmation",
    Component: BookingConfirmationScreen,
  },
  {
    path: "/booking/success",
    Component: BookingSuccessScreen,
  },
  {
    path: "/bookings",
    Component: BookingsScreen,
  },
  {
    path: "/history",
    Component: HistoryScreen,
  },
  {
    path: "/loyalty",
    Component: LoyaltyScreen,
  },
  {
    path: "/rating",
    Component: RatingScreen,
  },
  {
    path: "/vehicles",
    Component: VehiclesScreen,
  },
  {
    path: "/profile",
    Component: ProfileScreen,
  },
  {
    path: "/contact",
    Component: ContactScreen,
  },
]);
