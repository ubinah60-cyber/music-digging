package com.example.musicdigging.mapper;

import com.example.musicdigging.domain.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MusicMapper {

    List<Music> findAll();

    List<Music> search(@Param("keyword") String keyword);
}