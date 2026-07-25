package com.space.space_bundle.service;

import com.space.space_bundle.entity.Announcement;
import com.space.space_bundle.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    public Announcement createAnnouncement(String title, String message, boolean active) {
        Announcement announcement = Announcement.builder()
                .title(title)
                .message(message)
                .active(active)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return announcementRepository.save(announcement);
    }

    public Announcement updateAnnouncement(String id, String title, String message, boolean active) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found"));
        announcement.setTitle(title);
        announcement.setMessage(message);
        announcement.setActive(active);
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(String id) {
        announcementRepository.deleteById(id);
    }
}
