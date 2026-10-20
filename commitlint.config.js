/** @type {import('@commitlint/types').UserConfig} */
module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "subject-case": [0, "always"]
  },
  // semantic-release's own "chore(release): x.y.z [skip ci]" commit is machine-generated:
  // its body is an auto-generated changelog full of long commit-link URLs, which always
  // trips body-max-line-length. Skip linting it entirely instead of relaxing rules for
  // everyone else's commits.
  ignores: [(commit) => /^chore\(release\):/.test(commit) || commit.includes("[skip ci]")]
};
