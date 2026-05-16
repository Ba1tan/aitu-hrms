import { useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SETUP_STEPS, stepIndex } from "./setupSteps";

export function SetupNav({
  current,
  onNext,
  nextLabel,
  nextDisabled,
  nextLoading,
  skip,
}: {
  current: string;
  onNext?: () => Promise<void> | void;
  nextLabel?: string;
  nextDisabled?: boolean;
  nextLoading?: boolean;
  skip?: boolean;
}) {
  const navigate = useNavigate();
  const idx = stepIndex(current);
  const prev = idx > 0 ? SETUP_STEPS[idx - 1] : null;
  const next = idx < SETUP_STEPS.length - 1 ? SETUP_STEPS[idx + 1] : null;

  const goNext = async () => {
    if (onNext) {
      try {
        await onNext();
      } catch {
        return;
      }
    }
    if (next) navigate(`/setup/${next.path}`);
  };

  const goPrev = () => {
    if (prev) navigate(`/setup/${prev.path}`);
  };

  return (
    <div
      style={{
        marginTop: 32,
        display: "flex",
        justifyContent: "space-between",
        gap: 12,
      }}
    >
      {prev ? (
        <Button variant="outline" onClick={goPrev} disabled={nextLoading}>
          <ArrowLeft className="h-4 w-4 mr-2" /> Назад
        </Button>
      ) : (
        <div />
      )}
      <div style={{ display: "flex", gap: 8 }}>
        {skip && (
          <Button variant="ghost" onClick={() => next && navigate(`/setup/${next.path}`)}>
            Пропустить
          </Button>
        )}
        <Button onClick={goNext} disabled={nextDisabled || nextLoading}>
          {nextLoading
            ? "Сохранение…"
            : nextLabel ?? "Далее"}
          <ArrowRight className="h-4 w-4 ml-2" />
        </Button>
      </div>
    </div>
  );
}
