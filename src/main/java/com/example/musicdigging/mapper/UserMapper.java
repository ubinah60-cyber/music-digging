package com.example.musicdigging.mapper;

import com.example.musicdigging.domain.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface UserMapper {
    List<User> findAll();
}