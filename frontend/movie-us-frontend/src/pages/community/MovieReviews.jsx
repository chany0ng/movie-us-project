import { Box, Heading, Divider, Input, InputGroup, InputLeftElement } from "@chakra-ui/react";
import SimpleMovieGrid from "../../components/SimpleMovieGrid";
import { getData } from "../../api/axios";
import { useEffect, useState, useCallback } from "react";
import { useToast } from "@chakra-ui/react";
import styled from "styled-components";
import { SearchIcon } from "@chakra-ui/icons";
import { useNavigate } from "react-router-dom";
import ReportButton from '../../components/ReportButton';

const MovieReviews = () => {
  const [reviews, setReviews] = useState([]);
  const [topMovies, setTopMovies] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const userNum = 1; // 임시 유저 번호 설정
  const toast = useToast();
  const navigate = useNavigate();

  const filteredReviews = reviews
    .filter((review) => {
      const searchLower = searchTerm.toLowerCase();
      return (
        review.title?.toLowerCase().includes(searchLower) ||
        review.comment?.toLowerCase().includes(searchLower)
      );
    })
    .sort((a, b) => new Date(b.reviewDate) - new Date(a.reviewDate));

  const processTopMovies = useCallback((reviewsData) => {
    const movieReviewCounts = reviewsData.reduce((acc, review) => {
      const { tmdbId, posterPath, rating } = review;
      if (!acc[tmdbId]) {
        acc[tmdbId] = {
          movieId: tmdbId,
          posterPath,
          reviewCount: 0,
          totalRating: 0,
          averageRating: 0
        };
      }
      acc[tmdbId].reviewCount += 1;
      acc[tmdbId].totalRating += rating;
      acc[tmdbId].averageRating = acc[tmdbId].totalRating / acc[tmdbId].reviewCount;
      return acc;
    }, {});

    const sortedMovies = Object.values(movieReviewCounts)
      .sort((a, b) => b.reviewCount - a.reviewCount)
      .slice(0, 5);

    setTopMovies(sortedMovies);
    setIsLoading(false);
  }, []);

  const fetchReviews = useCallback(async () => {
    try {
      const response = await getData("/api/review/reviewList");
      setReviews(response.data);
      processTopMovies(response.data);
    } catch (error) {
      toast({
        title: "리뷰 조회 실패",
        description: `Failed to fetch reviews / ${error}`,
        status: "error",
        duration: 2000,
        isClosable: true,
      });
      console.error("Error fetching reviews:", error);
    }
  }, [toast, processTopMovies]);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  const handleSearch = (value) => {
    setSearchTerm(value);
  };

  const handleReviewClick = (tmdbId) => {
    navigate(`/movie-detail/${tmdbId}`);
  };
  
  return (
    <Box>
      <Box p={4}>
        <Heading
          fontSize="2xl"
          mt={10}
          mb={6}
          color="white"
          fontFamily={"NanumSquareRound"}
        >
          리뷰 수 TOP 5
        </Heading>
        <SimpleMovieGrid 
          movies={topMovies} 
          isLoading={isLoading}
          showReviewCount={true}
          showRating={true}
        />
      </Box> 
      <Divider borderColor="#3F3F3F" />
      
      <Box p={4}>
        <HeaderSection>
          <Heading fontSize="2xl" mt={10} mb={6} color="white" fontFamily={"NanumSquareRound"}>
            최신 리뷰
          </Heading>
          
          <SearchBar>
            <InputGroup size="md" width="300px">
              <InputLeftElement pointerEvents="none">
                <SearchIcon color="gray.300" />
              </InputLeftElement>
              <Input 
                placeholder="리뷰 검색 (영화제목, 내용)"
                onChange={(e) => handleSearch(e.target.value)}
                bg="#2D2D2D"
                color="white"
                border="none"
                _placeholder={{ color: 'gray.400' }}
              />
            </InputGroup>
          </SearchBar>
        </HeaderSection>

        <ReviewGrid>
          {filteredReviews.map((review) => (
            <ReviewCard 
              key={review.reviewId} 
              onClick={() => handleReviewClick(review.tmdbId)}
            >
              <PosterSection>
                {review.posterPath && (
                  <img 
                    src={`https://image.tmdb.org/t/p/w500${review.posterPath}`}
                    alt="Movie poster"
                  />
                )}
              </PosterSection>
              <ReviewContent>
                <h3>영화 : {review.title}</h3>
                <ReviewInfo>
                  <div>User {review.userNum}</div>
                  <div style={{ color: review.rating >= 5 ? '#FFD700' : '#FF0000' }}>
                    ★ {review.rating?.toFixed(1)}
                  </div>
                </ReviewInfo>
                <p>{review.comment}</p> 
                <ReviewFooter>
                  <ReviewDate>
                    {new Date(review.reviewDate).toLocaleDateString()}
                  </ReviewDate>
                  {userNum !== review.userNum && (
                    <ReportButton reviewId={review.reviewId} />
                  )}
                </ReviewFooter>
              </ReviewContent>
            </ReviewCard>
          ))}
        </ReviewGrid>
      </Box>
    </Box>
  );
};

const ReviewGrid = styled.div`
  display: grid;
  gap: 20px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  grid-template-columns: repeat(3, 1fr);

  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(1, 1fr);
  }
`;

const ReviewCard = styled.div`
  width: 100%;
  max-width: 330px;
  aspect-ratio: 8/9;
  background: #2D2D2D;
  border-radius: 8px;
  overflow: hidden;
  color: white;
  cursor: pointer;
  transition: transform 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;

  &:hover {
    transform: translateY(-5px);
  }
`;

const PosterSection = styled.div`
  width: 100%;
  height: 40%;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

const ReviewContent = styled.div`
  padding: 16px;
  height: 60%;
  display: flex;
  flex-direction: column;

  h3 {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 8px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  p {
    font-size: 14px;
    margin: 8px 0;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    text-overflow: ellipsis;
    line-height: 1.5;
    height: 4.5em;
    flex: none;
  }
`;

const ReviewInfo = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: bold;
`;

const ReviewDate = styled.div`
  font-size: 14px;
  color: #888;
`;

const SearchBar = styled.div`
  margin-top: 10px;
`;

const HeaderSection = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
`;

const ReviewFooter = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
`;

export default MovieReviews;