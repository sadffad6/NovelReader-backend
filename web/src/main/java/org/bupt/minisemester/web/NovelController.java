package org.bupt.minisemester.web;

import jakarta.servlet.http.HttpServletRequest;
import org.bupt.minisemester.common.jwt.JwtToken;
import org.bupt.minisemester.common.jwt.JwtUtil;
import org.bupt.minisemester.common.util.R;
import org.bupt.minisemester.dao.DTO.ChapterDTO;

import org.bupt.minisemester.dao.entity.Novel;
import org.bupt.minisemester.dao.entity.User;
import org.bupt.minisemester.dao.mapper.NovelMapper;
import org.bupt.minisemester.dao.mapper.userMapper;
import org.bupt.minisemester.service.NovelServiceGlobal;
import org.bupt.minisemester.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.*;
import java.lang.*;

@RestController
@RequestMapping("/books")
public class NovelController {

    @Autowired
    private NovelServiceGlobal novelService;

    @Autowired
    private userMapper userMapper;

    private boolean status;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private NovelMapper novelMapper;

    @GetMapping("/{id}")
    public R getNovel(@PathVariable int id) {
        return R.ok(novelService.getBookUploaded(id));
    }

    @JwtToken
    @PostMapping("/import")
    public R importNovel(@RequestParam("file") MultipartFile file, @RequestParam("status") String status, HttpServletRequest request) {
        try {
            boolean status_ = Boolean.parseBoolean(status);
            System.out.println("接收到文件：" + file.getOriginalFilename());
            //以文件名作为title
            String title = file.getOriginalFilename();
            if (title == null || title.isEmpty()) {
                return R.failure("文件名不能为空");
            }
            //去除扩展名
            title = title.replaceAll("\\.txt$", "");

            //读取内容
            String content = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String token = request.getHeader("Authorization");
            token = token.substring(7);

            String userId = jwtUtil.getTokenClaims(token, "uid");
            if (userId == null || userId.isEmpty()) {
                return R.failure("无法获取用户信息");
            }

            User user = UserService.getUserByUid(userId);
            if (user == null) {
                return R.failure("该用户不存在");
            }

            System.out.println("开始导入小说：" + title);
            novelService.importNovel(title, content, user, status_);
            System.out.println("小说导入成功");
            return R.ok("小说导入成功");
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    @GetMapping("/read")
    public R getChapterContent(@RequestParam("book_id") Integer book_id, @RequestParam("relativeId") Integer relativeId) {
        ChapterDTO chapter = novelService.getChapter(book_id, relativeId);
        if (chapter != null) {
            return R.ok(chapter);
        } else {
            return R.failure("Chapter not found");
        }
    }

    @GetMapping("/cancel_star")
    public R cancelStar(@RequestParam("book_id") Integer book_id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        token = token.substring(7);

        String userId = jwtUtil.getTokenClaims(token, "uid");
        if (userId == null || userId.isEmpty()) {
            return R.failure("无法获取用户信息");
        }

        User user = UserService.getUserByUid(userId);
        if (user == null) {
            return R.failure("该用户不存在");
        }

        Novel novel = novelMapper.findById(book_id);
        if (novel == null) {
            return R.failure("该书籍不存在");
        }

        int result = userMapper.cancelStarNovel(user.getUserId(), book_id);
        if (result > 0) {
            return R.ok("成功取消该书籍");
        } else {
            return R.failure("取消收藏失败");
        }
    }

    @GetMapping("/star")
    public R starNovel(@RequestParam("book_id") Integer book_id, HttpServletRequest request) {
        try {
            //获取用户
            String token = request.getHeader("Authorization");
            token = token.substring(7);

            String userId = jwtUtil.getTokenClaims(token, "uid");
            if (userId == null || userId.isEmpty()) {
                return R.failure("无法获取用户信息");
            }

            User user = UserService.getUserByUid(userId);
            if (user == null) {
                return R.failure("该用户不存在");
            }

            Novel novel = novelMapper.findById(book_id);
            if (novel == null) {
                return R.failure("该书籍不存在");
            }

            if (user.getStar_novels() == null) {
                user.setStar_novels(new ArrayList<>());
            }
            System.out.println(user.getStar_novels().contains(novel.getId()));

            if (!user.getStar_novels().contains(novel.getId())) {
                user.addStarNovel(novel);
                for (Integer novelId : user.getStar_novels()) {
                    userMapper.addStarNovel(user.getUserId(), novelId);
                }
                return R.ok("书籍添加成功");
            } else {
                return R.failure("该书籍已经被收藏");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.failure(e.getMessage());
        }
    }

    @GetMapping("/status/{id}")
    public R getStarStatus(@PathVariable("id") int book_id, HttpServletRequest request) {
        //获取用户
        try {

            String token = request.getHeader("Authorization");
            token = token.substring(7);

            String userId = jwtUtil.getTokenClaims(token, "uid");
            if (userId == null || userId.isEmpty()) {
                return R.failure("无法获取用户信息");
            }
            boolean isStarred = UserService.isBookStarred(userId, book_id);
            return R.ok(isStarred);

        } catch (Exception e) {
            e.printStackTrace();
            return R.failure(e.getMessage());
        }

    }

    @JwtToken
    @GetMapping("")
    public R getAllNovels() {
        try {
            return R.ok("获取小说列表成功！", novelService.findAllNovels());
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
    }

    @JwtToken
    @GetMapping("/starred_novels")
    public R getStarredNovels(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        token = token.substring(7);

        String userId = jwtUtil.getTokenClaims(token, "uid");
        System.out.println(userId);
        if (userId == null || userId.isEmpty()) {
            return R.failure("无法获取用户信息");
        }

        User user = UserService.getUserByUid(userId);
        if (user == null) {
            return R.failure("该用户不存在");
        }

        return R.ok("ok", novelService.findStarredNovels(userId));
    }

    @JwtToken
    @PostMapping("/addinfo")
    public R addBookUploaded(@RequestParam("id") Integer id,
                             @RequestParam(value = "author", required = false) String author,
                             @RequestParam(value = "title", required = false) String title,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "novelType", required = false) String novelType,
                             @RequestParam(value = "picture", required = false) String picture) {
        try {
            Novel novel = novelMapper.findById(id);
            if (novel == null) {
                return R.failure("书籍不存在");
            }
            System.out.println(novel);

            if (title != null) {
                novel.setTitle(title);
            }
            if (author != null) {
                novel.setAuthor(author);
            }
            if (novelType != null) {
                novel.setNoveltype(novelType);
            }
            if (description != null) {
                novel.setDescription(description);
            }
            if (picture != null) {
                novel.setPicture(picture);
            }

            novelMapper.updateBookinfo(novel);
            return R.ok("书籍更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return R.failure(e.getMessage());
        }

    }
}
