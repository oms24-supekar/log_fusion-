import { useEffect, useState } from "react";
import "./intro-animation.css";

const letters = ["L", "O", "G", "_", "F", "U", "S", "I", "O", "N"];

export default function IntroAnimation({ onFinish }) {
  const [hide, setHide] = useState(false);

  useEffect(() => {
    const t1 = setTimeout(() => setHide(true), 3800);
    const t2 = setTimeout(() => {
      if (onFinish) onFinish();
    }, 4300);

    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [onFinish]);

  return (
    <div className={`intro-screen ${hide ? "intro-hide" : ""}`}>
      <div className="intro-center">
        <div className="logo-stage">
          <img src="/logo.png" alt="LogFusion Logo" className="intro-logo" />

          <div className="scatter scatter-1"></div>
          <div className="scatter scatter-2"></div>
          <div className="scatter scatter-3"></div>
          <div className="scatter scatter-4"></div>
          <div className="scatter scatter-5"></div>
          <div className="scatter scatter-6"></div>
          <div className="scatter scatter-7"></div>
          <div className="scatter scatter-8"></div>
        </div>

        <div className="word-mark">
          {letters.map((char, index) => (
            <span
              key={index}
              className="letter"
              style={{ "--i": index }}
            >
              {char}
            </span>
          ))}
        </div>

        <div className="tagline">Universal Log Pre-processing Framework</div>
      </div>
    </div>
  );
}