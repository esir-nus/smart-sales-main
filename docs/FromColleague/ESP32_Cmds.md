this is the communication protocal between the app (we are builidng) and the hardware (the badge that connects to the app) the funtions are mostly realized of both esp 32 and the app.  
ESP32 doesn't have an UI, it s only backend

1.App查询wifi status from the badge：
    thorugh BLE, APP发送：wifi#address#ip#name   //the message is fixed, (wifi, address, ip, name are a placehoders saved for actaully wifi name)
    ESP32返回：   
        第一条指令：IP#192.168.0.101   //the app and badge transmit files to each other via the same wifi network, so this is to help verify if the two ends stay in teh same network. the actual address depends
        第二条指令：SD#MstRobot    //this tells app the name of the network the badge is in.  the actual network name/or wifi name depends
2.Badge 连接新wifi指令：
    第一条指令：SD#MstRobot // the app has a interface to which the user can manually input the wifi name(not ssid, the user has no idea what ssid is)(MstRobot is a placehoder saved for actaully wifi name)
    第二条指令：PD#Cai123456 // the app has a interface to which the user can manually input the wifi password, (Cai123456 is a placehoder saved for actaully wifi name)
3.App发送gif照片集合to teh badge 指令流程步骤
    1. APP发送：jpg#send  // fixed command
    2. Esp32返回：jpg#receive // fixed command
    3. APP调用web传输照片集 // see @api-contract.md
    4. APP传输照片结束发送：jpg#end // everytime a photo is sent, the app will send a end command to the badge to notify it that the photo is sent
    5. Esp32返回：jpg#display //this means the badge has received the photo and is ready to display it
4.App获取wav音频文件指令流程步骤
    1. APP发送：wav#get //the app sends this command to inform the badge it wants to get the audio file
    2. Esp32返回：wav#send //the badge sends this command to inform the app it is ready to send the audio file
    3. APP调用web传输wav音频文件 //the app calls the web api to transfer the audio file. 
    4. APP传输照片结束发送：wav#end //everytime a photo is sent, the app will send a end command to the badge to notify it that the photo is sent
    5. Esp32返回：wav#ok
5.Esp32获取时间戳： // this is for ESP32 to get the time sync from the app 
    ESP32发送：time#get
App返回：time#20251227151356 //this is how the app should return the real time and the pattern YYYYMMDDHHMMSS
其余需要注意的地方： // so we need a integrated simple script to break down GIFs fowlloing the below requirement 
    1. 手机APP在Gif拆分为jpg格式图片文件的时候，按照帧数从数字1开始往后编排，比如gif中有25帧jpg图片，则拆分的文件名依次  为：1.jpg  2.jpg  3.jpg   4.jpg等等。  
    屏幕分辨为240*280，gif拆分图片帧之后，需要将jpg文件的分辨率大小，修改为240*280