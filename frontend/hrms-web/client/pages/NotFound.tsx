import { Link } from "react-router-dom";
import { ArrowLeft, Home } from "lucide-react";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-primary/5 flex items-center justify-center px-4 py-12">
      <div className="text-center space-y-8 max-w-md">
        {/* Logo */}
        <div className="flex justify-center">
          <div className="w-16 h-16 bg-primary rounded-lg flex items-center justify-center text-primary-foreground font-bold text-3xl">
            HR
          </div>
        </div>

        {/* Content */}
        <div className="space-y-4">
          <h1 className="font-display text-6xl font-bold text-foreground">404</h1>
          <h2 className="font-display text-2xl font-bold text-foreground">
            Page Not Found
          </h2>
          <p className="text-muted-foreground">
            Sorry, we couldn't find the page you're looking for. It might have been moved or deleted.
          </p>
        </div>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            to="/"
            className="inline-flex items-center justify-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90 px-6 py-3 rounded-lg font-semibold transition-colors"
          >
            <Home className="w-4 h-4" />
            Go Home
          </Link>
          <button
            onClick={() => window.history.back()}
            className="inline-flex items-center justify-center gap-2 border border-border text-foreground hover:bg-muted px-6 py-3 rounded-lg font-semibold transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Go Back
          </button>
        </div>

        {/* Decorative Elements */}
        <div className="pt-8 text-muted-foreground text-sm space-y-2">
          <p>Looking for something?</p>
          <div className="flex gap-4 justify-center text-xs">
            <a href="#" className="hover:text-foreground transition-colors">
              Documentation
            </a>
            <span className="text-border">•</span>
            <a href="#" className="hover:text-foreground transition-colors">
              Support
            </a>
            <span className="text-border">•</span>
            <a href="#" className="hover:text-foreground transition-colors">
              Status
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
