package com.ssafy.urturn.problem.repository;

import com.ssafy.urturn.member.Level;
import com.ssafy.urturn.problem.dto.ProblemTestcaseDto;
import com.ssafy.urturn.problem.entity.Problem;
import java.util.List;
import java.util.Optional;

public interface ProblemCustomRepository {

    Optional<ProblemTestcaseDto> getProblemAndTestcase(Long problemId);
    Optional<ProblemTestcaseDto> getProblemWithPublicTestcase(Long problemId);

    public List<ProblemTestcaseDto> selectTwoProblemByLevel(Level level, Long memberId, Long pairId);

}
