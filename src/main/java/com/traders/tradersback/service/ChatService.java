package com.traders.tradersback.service;

import com.traders.tradersback.dto.ChatMessageResponseDTO;
import com.traders.tradersback.model.ChatMessage;
import com.traders.tradersback.model.ChatRoom;
import com.traders.tradersback.model.Member;
import com.traders.tradersback.repository.ChatMessageRepository;
import com.traders.tradersback.repository.ChatRoomRepository;
import com.traders.tradersback.repository.MemberRepository;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private MemberRepository memberRepository;

    public ChatMessage saveMessage(ChatMessage chatMessage) {
        // 채팅방 존재 여부 확인
        if (!chatRoomRepository.existsById(chatMessage.getChatRoomId())) {
            throw new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + chatMessage.getChatRoomId());
        }

        try {
            chatMessage.setTimestamp(LocalDateTime.now());
            return chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            throw new ServiceException("메시지 저장 중 오류가 발생했습니다.", e);
        }
    }


    public List<ChatMessageResponseDTO> getMessagesByChatRoomIdWithNames(Long chatRoomId) {
        logger.debug("Fetching messages for chat room ID: {}", chatRoomId);
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId);
        if (messages.isEmpty()) {
            logger.info("No messages found for chatRoomId: {}. Returning empty list.", chatRoomId);
            return new ArrayList<>();
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found: " + chatRoomId));
        Member seller = memberRepository.findById(chatRoom.getSellerId())
                .orElseThrow(() -> new EntityNotFoundException("Seller not found: " + chatRoom.getSellerId()));
        Member buyer = memberRepository.findById(chatRoom.getBuyerId())
                .orElseThrow(() -> new EntityNotFoundException("Buyer not found: " + chatRoom.getBuyerId()));

        return messages.stream().map(message -> {
            String senderName;
            String otherPartyName;

            if (message.getSenderId().equals(seller.getMemberNum())) {
                senderName = seller.getMemberName();
                otherPartyName = buyer.getMemberName();
            } else if (message.getSenderId().equals(buyer.getMemberNum())) {
                senderName = buyer.getMemberName();
                otherPartyName = seller.getMemberName();
            } else {
                // 이 경우는 채팅 메시지의 송신자가 현재 채팅방의 구매자나 판매자와 일치하지 않음을 의미합니다.
                // 이러한 상황을 어떻게 처리할지는 애플리케이션의 요구사항에 따라 다릅니다.
                // 예를 들어, '알 수 없음'으로 처리할 수 있습니다.
                logger.warn("Message sender does not match buyer or seller in chatRoomId: {}", chatRoomId);
                senderName = "알 수 없음";
                otherPartyName = "알 수 없음";
            }

            return new ChatMessageResponseDTO(senderName, otherPartyName, message.getMessage(), message.getChatRoomId(), message.getTimestamp());
        }).collect(Collectors.toList());
    }

}
