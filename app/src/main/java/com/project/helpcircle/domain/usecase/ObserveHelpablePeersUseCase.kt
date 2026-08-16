package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityObservation
import com.project.helpcircle.domain.model.HelpablePeers
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Streams the peers this device may currently send a nudge to — see [HelpablePeers] for why that's
 * narrower than the full roster.
 *
 * The device's own identity is resolved once, before the community listener is subscribed, and then
 * reused for every emission: it never changes for the lifetime of an install, so re-reading it per
 * emission would add a storage round trip to each live community update for no benefit.
 */
class ObserveHelpablePeersUseCase(
    private val observeCommunityStateUseCase: ObserveCommunityStateUseCase,
    private val userRepository: UserRepository
) {
    operator fun invoke(communityId: String): Flow<HelpablePeers> = flow {
        val selfAnonymousId = userRepository.getOrCreateIdentity().anonymousHash
        emitAll(
            observeCommunityStateUseCase(communityId).map { observation ->
                when (observation) {
                    is CommunityObservation.Populated ->
                        HelpablePeers.from(observation.state.members, selfAnonymousId)
                    // A circle of one has no peers to help, and this device is the one member.
                    is CommunityObservation.SoloMode -> HelpablePeers.EMPTY
                }
            }
        )
    }
}
