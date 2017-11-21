Fs = 48000;
y = [];

% str = 'Tsinghua';
fid = fopen('1.txt', 'r');
tline = fgetl(fid);
while ~feof(fid)
    tline = strcat(tline, fgetl(fid));
end
fclose(fid);

[~,sizeN] = size(tline);
info = [];

for k=1:sizeN
    c = tline(k);
    newi = dec2bin(c,8);
    info = [info newi];
end

info = [info '0'];

[~,infol] = size(info);
for i = 1:2:infol
    tmpim = getImpulse(Fs,18000);
    z = [];
    if i == infol
        if info(i) == '0'
            z = zeros(1,400);
        elseif info(i) == '1'
            z = zeros(1,1200);
        end
    elseif info(i) == '0' && info(i+1) == '0'
        z = zeros(1,400);
    elseif info(i) == '0' && info(i+1) == '1'
        z = zeros(1,800);
    elseif info(i) == '1' && info(i+1) == '0'
        z = zeros(1,1200);
    elseif info(i) == '1' && info(i+1) == '1'
        z = zeros(1,1600);
    end
    y = [y tmpim];
    y = [y z];
end
sound(y,Fs);
audiowrite('test.wav', y, Fs);
