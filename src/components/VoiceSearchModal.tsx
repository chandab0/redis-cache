import React, { useEffect, useState, useRef } from 'react';
import { Mic, MicOff, X } from 'lucide-react';

interface VoiceSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscriptReady: (transcript: string) => void;
}

export const VoiceSearchModal: React.FC<VoiceSearchModalProps> = ({
  isOpen,
  onClose,
  onTranscriptReady,
}) => {
  const [transcript, setTranscript] = useState<string>('');
  const [isListening, setIsListening] = useState<boolean>(false);
  const [statusText, setStatusText] = useState<string>('Listening...');
  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    if (!isOpen) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      setIsListening(false);
      setTranscript('');
      return;
    }

    // Initialize Web Speech API if supported
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setStatusText('Speech recognition not supported in this browser. Please type your query.');
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.lang = 'en-US';

      recognition.onstart = () => {
        setIsListening(true);
        setStatusText('Listening... Speak now');
      };

      recognition.onresult = (event: any) => {
        let currentTranscript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          currentTranscript += event.results[i][0].transcript;
        }
        setTranscript(currentTranscript);

        if (event.results[0].isFinal) {
          setStatusText('Recognized! Searching...');
          setTimeout(() => {
            onTranscriptReady(currentTranscript);
            onClose();
          }, 600);
        }
      };

      recognition.onerror = (event: any) => {
        setIsListening(false);
        if (event.error === 'no-speech') {
          setStatusText('No speech was detected. Tap the mic to try again.');
        } else if (event.error === 'not-allowed') {
          setStatusText('Microphone permission denied. Please enable mic access.');
        } else {
          setStatusText(`Error: ${event.error}. Tap mic to retry.`);
        }
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch {
      setStatusText('Could not start voice recognition.');
    }

    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const restartListening = () => {
    if (recognitionRef.current) {
      setTranscript('');
      recognitionRef.current.start();
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-lg w-full p-8 shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col items-center text-center relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-2">
          {statusText}
        </h3>

        <p className="text-lg font-medium text-slate-800 dark:text-slate-200 h-16 flex items-center justify-center max-w-md px-4 overflow-hidden text-ellipsis">
          {transcript ? `"${transcript}"` : 'Say something like "Weather in Paris" or "Latest quantum physics news"'}
        </p>

        {/* Animated Google Colored Sound Wave Orbs */}
        <div className="my-8 flex items-center gap-3">
          {isListening ? (
            <div className="flex items-center gap-2.5 h-16">
              <span className="w-4 h-4 rounded-full bg-[#4285F4] animate-bounce" style={{ animationDelay: '0ms', animationDuration: '600ms' }} />
              <span className="w-4 h-4 rounded-full bg-[#EA4335] animate-bounce" style={{ animationDelay: '150ms', animationDuration: '600ms' }} />
              <span className="w-4 h-4 rounded-full bg-[#FBBC05] animate-bounce" style={{ animationDelay: '300ms', animationDuration: '600ms' }} />
              <span className="w-4 h-4 rounded-full bg-[#34A853] animate-bounce" style={{ animationDelay: '450ms', animationDuration: '600ms' }} />
            </div>
          ) : (
            <button
              onClick={restartListening}
              className="w-20 h-20 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border-2 border-blue-200 dark:border-blue-800 flex items-center justify-center hover:scale-105 transition-transform cursor-pointer shadow-lg"
            >
              <Mic className="w-8 h-8" />
            </button>
          )}
        </div>

        <p className="text-xs text-slate-400">
          Powered by Web Speech API & Voice AI
        </p>
      </div>
    </div>
  );
};
