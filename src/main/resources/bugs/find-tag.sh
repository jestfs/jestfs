grep "$1" -r "${2:-TODO}" -l | xargs cat | sed "s/\"use/\n\n\"use/g" | vim -
