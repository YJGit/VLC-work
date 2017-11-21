%% 对使用 pulse coding 编码传输之后的数据进行解码。

%% 解码
[data, fs] = audioread('myaudio_ok.wav');
data = data(:,1);
figure(1);
plot(data);
figure(2);
plot(data);
hold on;

hd = design(fdesign.bandpass('N,F3dB1,F3dB2',6,14000,20000,fs),'butter');
data = filter(hd,data);

f = 18000;


[n,~] = size(data);
L = 100;
impulse_fft = zeros(n,1);
for i= 1:1:n-L
    y = fft(data(i:i+L-1));
    y = abs(y);
    index_impulse = round(f/48000*L);
    impulse_fft(i)=max(y(index_impulse-2:index_impulse+2));
end

figure(3);
plot(impulse_fft);

% 滑动平均
for i= 6:1:n-5
    impulse_fft(i)=mean(impulse_fft(i-5:i+5));
end

figure(4);
hold on; 
plot(impulse_fft);

% 取出impulse 中心
poLen = 0;
position_impulse=[];

for i= 51:1:n-51
    if impulse_fft(i)> 0.3 && impulse_fft(i)==max(impulse_fft(i-50:i+50))
        if(poLen == 0)
            position_impulse=[position_impulse,i];
            poLen = poLen + 1;
        else
            if(i - position_impulse(poLen) > 100)
                position_impulse=[position_impulse,i];
                poLen = poLen + 1;
            end 
        end
    end
end

%{
for i= 1:1:n-101
    if(impulse_fft >= 0.5)
        if(i >= 100 && impulse_fft(i) == max(impulse_fft(i - 100: i + 100)))
            position_impulse=[position_impulse,i];
        end
        if(i < 100 && impulse_fft(i) == max(impulse_fft(0: i + 100)))
            position_impulse=[position_impulse,i];
        end
    end
end
%}

[~,N]= size(position_impulse);
delta_impulse=zeros(1,N-1);
for i = 1:N-1
    figure(4);
    plot([position_impulse(i),position_impulse(i)],[0,0.6],'m');
    figure(2);
    plot([position_impulse(i),position_impulse(i)],[0,0.3],'m','linewidth',2);
   delta_impulse(i) = position_impulse(i+1) -  position_impulse(i);
end
delta_impulse = delta_impulse-100;
decode_message4 = zeros(1,N-1)-1;
for i = 1:N-1
    if delta_impulse(i) - 400 >-20 &&delta_impulse(i) - 400 <20
        decode_message4(i) = 0;
    elseif delta_impulse(i) - 800 >-20 &&delta_impulse(i) - 800 <20
        decode_message4(i) = 1;
    elseif delta_impulse(i) - 1200 >-20 &&delta_impulse(i) - 1200 <20
        decode_message4(i) = 2;
    elseif delta_impulse(i) - 1600 >-20 &&delta_impulse(i) - 1600 <20
        decode_message4(i) = 3;
    end
end

decode_message = zeros(1,(N-1)*2)-1;
for i = 1:N-1
    if decode_message4(i) == 0
        decode_message(i*2-1)=0;
        decode_message(i*2)=0;
    elseif decode_message4(i) == 1
        decode_message(i*2-1)=0;
        decode_message(i*2)=1;
    elseif decode_message4(i) == 2
        decode_message(i*2-1)=1;
        decode_message(i*2)=0;
    elseif decode_message4(i) == 3
        decode_message(i*2-1)=1;
        decode_message(i*2)=1;
    end
end

str = bin2string(decode_message)


