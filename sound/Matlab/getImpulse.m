function y = getImpulse(Fs,f)
t = 0:1/Fs:99/Fs;
y = sin(2*pi*f*t);
% plot(t,y);
% sound(y,Fs);
% wavwrite(y,Fs,'mywav.wav');
end



