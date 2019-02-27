#ifndef __BENJMARK_H__
#define __BENJMARK_H__

#include "json.hpp"
#include <string>
#include <chrono>
#include <iostream>

namespace bj {

  nlohmann::json readJson(const std::string& filename);
  void writeJson(const std::string& filename, const nlohmann::json& data);

  template <typename Setup>
    void perform(
      const Setup& setup, 
      const std::string& input_filename, 
      const std::string& output_filename) {
    std::cout << "Load json" << std::endl;
    nlohmann::json input_json = readJson(input_filename);
    std::cout << "Import data" << std::endl;
    auto problem = setup.input(input_json["data"]);
    std::cout << "Dry run" << std::endl;
    auto dry_output = setup.compute(problem);
    std::cout << "Run" << std::endl;
    auto start = std::chrono::steady_clock::now();
    auto output = setup.compute(problem);
    auto stop = std::chrono::steady_clock::now();
    std::cout << "Export data" << std::endl;

    auto diff = stop - start;
    auto diff_us = std::chrono::duration_cast<std::chrono::microseconds>(stop - start).count();

    nlohmann::json results;
    results["time-seconds"] = 1.0e-6*diff_us;
    results["output"] = setup.output(output);
    results["dry-output"] = setup.output(dry_output);
    std::cout << "Save data" << std::endl;
    writeJson(output_filename, results);
    std::cout << "Done" << std::endl;
  }


}


#endif
