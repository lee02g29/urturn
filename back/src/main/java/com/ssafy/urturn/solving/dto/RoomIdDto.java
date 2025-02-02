package com.ssafy.urturn.solving.dto;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class RoomIdDto {
    private static final Logger log = LoggerFactory.getLogger(RoomIdDto.class);
    private String roomId;

    @Override
    public String toString() {
        return "RoomIdDto{" +
                "roomId='" + roomId + '\'' +
                '}';
    }
}
