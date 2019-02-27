#include "benjmark.h"
#include <fstream>

namespace bj {
  nlohmann::json readJson(const std::string& filename) {
    std::ifstream file(filename);
    nlohmann::json j;
    file >> j;
    return j;
  }

  void writeJson(const std::string& filename, const nlohmann::json& data) {
    std::ofstream file(filename);
    file << data;
  }
}
