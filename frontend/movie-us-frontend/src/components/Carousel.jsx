// import React from 'react';
import PropTypes from "prop-types";
import { Swiper, SwiperSlide } from "swiper/react";
import { Autoplay, Pagination, Navigation } from "swiper/modules";
import { Box, Image } from "@chakra-ui/react";

// Swiper 스타일 import
import "swiper/css";
import "swiper/css/pagination";
import "swiper/css/navigation";

const Carousel = ({ movies }) => {
  return (
    <Swiper
      modules={[Autoplay, Pagination, Navigation]}
      spaceBetween={30}
      slidesPerView={1} // 한 번에 보이는 슬라이드 수 조정
      autoplay={{
        delay: 2000,
        disableOnInteraction: false,
      }}
      pagination={{ clickable: true }}
      navigation={true}
      loop={true}
    >
      {movies.map((movie) => (
        <SwiperSlide key={movie.id}>
          <Box
            position="relative"
            margin="0 auto"
            width="100%" // 전체 화면 대비 가로 폭 줄이기
          >
            {/* 위쪽 그라데이션 추가 */}
            <Box
              sx={{
                position: "absolute",
                content: '""',
                top: 0,
                left: 0,
                right: 0,
                height: "50px", // 그라데이션 높이
                background:
                  "linear-gradient(to top, rgba(0, 0, 0, 0) 0%, black 75%, black 100%)",
                zIndex: 1, // 이미지 위에 오도록 설정
              }}
            />

            <Image
              src={movie.posterUrl}
              alt={movie.title}
              width="100%"
              height="800px"
              borderRadius="xl" // 모서리를 둥글게 처리
              objectFit="cover"
              zIndex={0} // 그라데이션 박스 아래에 위치
            />

            <Box
              sx={{
                position: "absolute",
                content: '""',
                bottom: 0,
                left: 0,
                right: 0,
                height: "100px", // 그라데이션 높이
                background:
                  "linear-gradient(to bottom, rgba(0, 0, 0, 0) 0%, black 75%, black 100%)",
                zIndex: 1, // 이미지 위에 오도록 설정
              }}
            />
          </Box>
        </SwiperSlide>
      ))}
    </Swiper>
  );
};

Carousel.propTypes = {
  movies: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      posterUrl: PropTypes.string.isRequired,
      title: PropTypes.string.isRequired,
    })
  ).isRequired,
};

export default Carousel;
