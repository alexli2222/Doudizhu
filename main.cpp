#include <iostream>

int main() {
    std::string line;

    while (std::getline(std::cin, line)) {
        if (line == "isready") {
            std::cout << "readyok" << std::endl;
        } else if (line == "go") {
            std::cout << "best move here" << std::endl;
        }
    }
}