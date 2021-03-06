package leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author spencercjh
 */
public class CombinationSum_39 {
    class Solution {
        public List<List<Integer>> combinationSum(int[] candidates, int target) {
            List<List<Integer>> answers = new ArrayList<>();
            Arrays.sort(candidates);
            find(answers, new ArrayList<>(), candidates, target, 0);
            return answers;
        }

        private void find(List<List<Integer>> answers, List<Integer> answer, int[] candidates, int target, int index) {
            if (target == 0) {
                answers.add(answer);
            }
            if (index >= candidates.length || target < candidates[index]) {
                return;
            }
            for (int i = index; i < candidates.length && candidates[i] <= target; i++) {
                List<Integer> list = new ArrayList<>(answer);
                list.add(candidates[i]);
                find(answers, list, candidates, target - candidates[i], i);
            }
        }
    }
}
