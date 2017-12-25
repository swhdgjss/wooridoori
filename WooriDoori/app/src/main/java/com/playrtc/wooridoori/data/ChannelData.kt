package com.playrtc.wooridoori.data

/*
 * 채널 정보를 담는 Value Class
 * - channelId : String 채널 아이디, PlayRTC 채널 서비스에서 발급
 * - channelName : String 채널의 이름, 채널 생성 시 부여한 이름
 * - userId : String 채널을 생성한 사용자 아이디, Application에서 사용하는 아이디
 * - userName : String 채널을 생성한 사용자 이름
 */
class ChannelData {
    /*
     * 채널 아이디, PlayRTC 채널 서비스에서 발급
     */
    var channelId: String? = null

    /*
     * 채널의 이름, 채널 생성 시 부여한 이름
     */
    var channelName: String? = null

    /*
     * 채널을 생성한 사용자 아이디, Application에서 사용하는 아이디
     */
    var userId: String? = null

    /*
     * 채널을 생성한 사용자 이름
     */
    var userName: String? = null
}