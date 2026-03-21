package pt.estga.content.services;

import pt.estga.content.dtos.BookmarkDto;
import pt.estga.content.enums.TargetType;

import java.util.List;

public interface BookmarkService {

    BookmarkDto createBookmark(Long userId, TargetType type, String targetId);

    List<BookmarkDto> getUserBookmarks(Long userId);

    void deleteBookmark(Long userId, Long bookmarkId);

    boolean isBookmarked(Long userId, TargetType type, String targetId);
}
