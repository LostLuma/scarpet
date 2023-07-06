// Fork of keepalive.sc
//
// Keeps carpet bots logged in while at least one human is online or in shadow mode.
// Once all humans leave the bots are stored on disk ready to be spawned again soon.

__config() -> {
    'scope' -> 'global'
};

__on_player_connects(player) -> {
    data = load_app_data();

    if (data && data:'players',
        data = parse_nbt(data:'players');

        for (data,
            for([str('player %s spawn at %f %f %f facing %f %f in %s',
                    _:'name', _:'x', _:'y', _:'z', _:'yaw', _:'pitch', _:'dim'),
                str('gamemode %s %s', _:'gm', _:'name')],
            run(_);
            );
            modify(player(_:'name'), 'flying', _:'fly')
        )
    );
};

__on_server_shuts_down() -> (
    save_and_remove_bots();
);

__on_player_disconnects(player, reason) -> {
    // Ignore duplicate login since it's most likely /player <name> shadow usage,
    // And it's a good indicator the player wants the server to keep being active
    if (player~'player_type' != 'fake' && reason != 'translation{key=\'multiplayer.disconnect.duplicate_login\', args=[]}',
        // Count amount of non-fake players
        // Note that the player which left is still included, substract them
        human = length(filter(player('all'), _~'player_type' != 'fake')) - 1;

        if (human == 0, save_and_remove_bots());
    );
};

save_and_remove_bots() -> {
    saved = [];
    data = nbt('{players:[]}');

    for (filter(player('all'), _~'player_type' == 'fake'),
        pdata = nbt('{}');
        pdata:'name' = _~'name';
        pdata:'dim' = _~'dimension';
        pdata:'x' = _~'x';
        pdata:'y' = _~'y';
        pdata:'z' = _~'z';
        pdata:'yaw' = _~'yaw';
        pdata:'pitch' = _~'pitch';
        pdata:'gm' = _~'gamemode';
        pdata:'fly' = _~'flying';

        saved += _~'name';
        put(data, 'players', pdata, -1);

        run(str('player %s kill', _~'name'));
    );

    store_app_data(data);
    if (saved, logger('info', 'Saved ' + saved + ' until a human logs in again.'));
}
